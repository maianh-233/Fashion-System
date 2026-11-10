package com.fashionsystem.fashion_system.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.CampaignRequest;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Detail;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.ExpenseRequest;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.LinkRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Chạy trên cơ sở dữ liệu thật với quyền và phạm vi thật; dữ liệu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class MarketingCampaignIntegrationTest {
    @Autowired MarketingCampaignService campaigns;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MarketingCampaignNotifier notifier;

    final UUID store = UUID.randomUUID();
    final UUID superAdmin = UUID.randomUUID();
    final UUID admin = UUID.randomUUID();
    final UUID admin2 = UUID.randomUUID();
    final UUID marketer = UUID.randomUUID();
    final UUID storeManager = UUID.randomUUID();
    final UUID storeManagerWithManage = UUID.randomUUID();
    String tag;

    @BeforeEach
    void setUp() {
        tag = store.toString().substring(0, 8);
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, ?, true, ?, ?)",
                store, "ZMC" + tag, "Zmc Store", 10 + Math.random(), 100 + Math.random());
        employee(superAdmin, "super", "SUPER_ADMIN", null);
        employee(admin, "admin", "ADMIN", null);
        employee(admin2, "admin2", "ADMIN", null);
        employee(marketer, "mkt", "MARKETING", null);
        employee(storeManager, "man", "MANAGER", store);
        // Nhân viên cửa hàng lỡ được gán role có MANAGE: vẫn không được ghi vì không phải cấp chuỗi.
        employee(storeManagerWithManage, "man2", "MARKETING", store);
    }

    // ------------------------------------------------------------------ chiến dịch và quy trình duyệt

    @Test
    void createValidatesWindowAndRequiresGlobalManager() {
        Detail created = create(marketer, "Sale 12.12");
        assertThat(created.campaign().code()).startsWith("MC-");
        assertThat(created.campaign().status()).isEqualTo("DRAFT");
        assertThat(created.campaign().ownerId()).isEqualTo(marketer);
        assertThat(created.history()).extracting(MarketingCampaignDtos.History::action).containsExactly("CREATED");

        LocalDateTime start = LocalDateTime.now().plusDays(5);
        assertStatus(() -> job(() -> campaigns.create(marketer, request("Sai giờ", start, start.minusHours(1)))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> job(() -> campaigns.create(marketer, request("Quá dài", start, start.plusDays(400)))), HttpStatus.BAD_REQUEST);
        // Phase 4: nhân viên cửa hàng có MANAGE tạo được chiến dịch, nhưng luôn gắn với cửa hàng của mình.
        assertThat(job(() -> campaigns.create(storeManagerWithManage, request("Cửa hàng", start, start.plusDays(3))))
                .campaign().storeId()).isEqualTo(store);
        assertStatus(() -> job(() -> campaigns.create(marketer, new CampaignRequest("Người phụ trách cửa hàng", "REVENUE", null,
                start, start.plusDays(3), storeManager, null, null, null, null, null))), HttpStatus.BAD_REQUEST);
    }

    @Test
    void approvalFlowWithEditLocksAndSelfApprovalRule() {
        UUID id = create(marketer, "Back to school").campaign().id();
        assertThat(job(() -> campaigns.submit(marketer, id)).campaign().status()).isEqualTo("PENDING_APPROVAL");
        verify(notifier).send(any(), eq(marketer), eq(MarketingCampaignNotifier.SUBMITTED), anyString(), anyString(), eq(id), anyString());

        // Đang chờ duyệt thì không sửa được.
        assertStatus(() -> job(() -> campaigns.update(marketer, id, request("Đổi tên", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(9)))), HttpStatus.CONFLICT);
        // Marketing không có quyền duyệt.
        assertStatus(() -> job(() -> campaigns.approve(marketer, id, null)), HttpStatus.FORBIDDEN);

        Detail rejected = job(() -> campaigns.reject(admin, id, "Thiếu ngân sách"));
        assertThat(rejected.campaign().status()).isEqualTo("REJECTED");
        assertThat(rejected.campaign().decisionNote()).isEqualTo("Thiếu ngân sách");
        assertThat(rejected.actions().approve()).isFalse();
        assertThat(job(() -> campaigns.update(marketer, id, request("Back to school 2", LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(9)))).campaign().name()).isEqualTo("Back to school 2");

        job(() -> campaigns.submit(marketer, id));
        Detail approved = job(() -> campaigns.approve(admin, id, "OK"));
        assertThat(approved.campaign().status()).isEqualTo("APPROVED");
        assertThat(approved.campaign().phase()).isEqualTo("UPCOMING");
        assertThat(approved.history()).extracting(MarketingCampaignDtos.History::action)
                .containsExactly("APPROVED", "SUBMITTED", "UPDATED", "REJECTED", "SUBMITTED", "CREATED");

        // Duyệt lần hai: không còn chờ duyệt.
        assertStatus(() -> job(() -> campaigns.approve(admin2, id, null)), HttpStatus.CONFLICT);

        // Mở lại trước khi bắt đầu → về Nháp.
        assertThat(job(() -> campaigns.reopen(marketer, id, "Đổi ưu đãi")).campaign().status()).isEqualTo("DRAFT");
    }

    @Test
    void adminCannotApproveOwnCampaignButSuperAdminCan() {
        UUID own = create(admin, "Của admin").campaign().id();
        job(() -> campaigns.submit(admin, own));
        assertThat(campaigns.detail(admin, own).actions().approve()).isFalse();
        assertStatus(() -> job(() -> campaigns.approve(admin, own, null)), HttpStatus.FORBIDDEN);
        assertThat(job(() -> campaigns.approve(admin2, own, null)).campaign().status()).isEqualTo("APPROVED");

        UUID superOwn = create(superAdmin, "Của super admin").campaign().id();
        job(() -> campaigns.submit(superAdmin, superOwn));
        Detail approved = job(() -> campaigns.approve(superAdmin, superOwn, "Gấp"));
        assertThat(approved.campaign().status()).isEqualTo("APPROVED");
        assertThat(approved.history().getFirst().note()).contains("SUPER_ADMIN tự duyệt").contains("Gấp");
    }

    @Test
    void reopenOnlyBeforeStartAndCannotApproveAfterEnd() {
        UUID id = create(marketer, "Đang chạy").campaign().id();
        job(() -> campaigns.submit(marketer, id));
        job(() -> campaigns.approve(admin, id, null));
        jdbc.update("update marketing_campaigns set starts_at = (now() at time zone 'UTC') - interval '1 day' where id = ?", id);
        assertThat(campaigns.detail(admin, id).campaign().phase()).isEqualTo("RUNNING");
        assertStatus(() -> job(() -> campaigns.reopen(marketer, id, "Muốn sửa")), HttpStatus.CONFLICT);

        UUID late = create(marketer, "Hết hạn khi chờ").campaign().id();
        job(() -> campaigns.submit(marketer, late));
        jdbc.update("update marketing_campaigns set starts_at = (now() at time zone 'UTC') - interval '3 day',"
                + " ends_at = (now() at time zone 'UTC') - interval '1 day' where id = ?", late);
        assertStatus(() -> job(() -> campaigns.approve(admin, late, null)), HttpStatus.CONFLICT);
        assertThat(job(() -> campaigns.reject(admin, late, "Đã quá hạn")).campaign().status()).isEqualTo("REJECTED");
        assertStatus(() -> job(() -> campaigns.submit(marketer, late)), HttpStatus.CONFLICT);
    }

    // ------------------------------------------------------------------ ưu đãi liên kết

    @Test
    void offerBelongsToOneCampaignAndOnlyAllowedTypesLink() {
        UUID first = create(marketer, "Chiến dịch A").campaign().id();
        UUID second = create(marketer, "Chiến dịch B").campaign().id();
        UUID promo = promotion("ORDER", "ACTIVE");

        Detail linked = job(() -> campaigns.link(marketer, first, new LinkRequest("PROMOTION", promo)));
        assertThat(linked.offers()).singleElement().satisfies(o -> {
            assertThat(o.itemId()).isEqualTo(promo);
            assertThat(o.inEffect()).isTrue();
            assertThat(o.detail()).contains("Giảm 10%");
        });
        assertThat(linked.history().getFirst().action()).isEqualTo("OFFER_LINKED");

        assertThatThrownBy(() -> job(() -> campaigns.link(marketer, second, new LinkRequest("PROMOTION", promo))))
                .hasMessageContaining("đã thuộc chiến dịch");
        assertStatus(() -> job(() -> campaigns.link(marketer, first, new LinkRequest("PROMOTION", promo))), HttpStatus.CONFLICT);
        assertStatus(() -> job(() -> campaigns.link(marketer, first, new LinkRequest("PROMOTION", promotion("BIRTHDAY", "ACTIVE")))),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> job(() -> campaigns.link(marketer, first, new LinkRequest("PROMOTION", promotion("ORDER", "INACTIVE")))),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> job(() -> campaigns.link(marketer, first, new LinkRequest("FLASH_SALE", flashSale(false)))),
                HttpStatus.BAD_REQUEST);

        // Ứng viên cho biết chiến dịch đang giữ ưu đãi.
        var candidate = campaigns.candidates(marketer, "PROMOTION", tag, PageRequest.of(0, 50)).getContent().stream()
                .filter(c -> c.itemId().equals(promo)).findFirst().orElseThrow();
        assertThat(candidate.linkedCampaignId()).isEqualTo(first);
        assertThat(candidate.reason()).contains("Đã thuộc chiến dịch");

        // Đã gửi duyệt thì không liên kết / gỡ được.
        job(() -> campaigns.submit(marketer, first));
        assertStatus(() -> job(() -> campaigns.link(marketer, first, new LinkRequest("FLASH_SALE", flashSale(true)))), HttpStatus.CONFLICT);
        UUID offerId = linked.offers().getFirst().id();
        assertStatus(() -> job(() -> campaigns.unlink(marketer, first, offerId)), HttpStatus.CONFLICT);
    }

    @Test
    void cancellingKeepsOffersActiveAndWarnsThenUnlinkFreesThem() {
        UUID id = create(marketer, "Sẽ hủy").campaign().id();
        UUID promo = promotion("PRODUCT", "ACTIVE");
        UUID flash = flashSale(true);
        job(() -> campaigns.link(marketer, id, new LinkRequest("PROMOTION", promo)));
        Detail withFlash = job(() -> campaigns.link(marketer, id, new LinkRequest("FLASH_SALE", flash)));
        job(() -> campaigns.submit(marketer, id));
        job(() -> campaigns.approve(admin, id, null));

        assertStatus(() -> job(() -> campaigns.cancel(marketer, id, "abc")), HttpStatus.BAD_REQUEST);
        Detail cancelled = job(() -> campaigns.cancel(marketer, id, "Đổi kế hoạch"));
        assertThat(cancelled.campaign().status()).isEqualTo("CANCELLED");
        assertThat(cancelled.warnings()).anyMatch(w -> w.contains("2 ưu đãi vẫn còn hiệu lực"));
        // Module Marketing không đụng vào ưu đãi.
        assertThat(jdbc.queryForObject("select status from promotions where id = ?", String.class, promo)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select active from flash_sales where id = ?", Boolean.class, flash)).isTrue();
        assertStatus(() -> job(() -> campaigns.cancel(marketer, id, "Hủy lần nữa")), HttpStatus.CONFLICT);

        // Gỡ liên kết ở chiến dịch đã hủy để dùng ưu đãi cho chiến dịch khác.
        UUID promoOffer = withFlash.offers().stream().filter(o -> o.itemId().equals(promo)).findFirst().orElseThrow().id();
        job(() -> campaigns.unlink(marketer, id, promoOffer));
        UUID other = create(marketer, "Chiến dịch mới").campaign().id();
        assertThat(job(() -> campaigns.link(marketer, other, new LinkRequest("PROMOTION", promo))).offers()).hasSize(1);
    }

    @Test
    void offerOutsideCampaignWindowIsOnlyAWarning() {
        UUID id = create(marketer, "Tháng sau").campaign().id();
        UUID expired = promotion("ORDER", "EXPIRED");
        jdbc.update("update promotions set start_date = (now() at time zone 'UTC') - interval '30 day',"
                + " end_date = (now() at time zone 'UTC') - interval '20 day' where id = ?", expired);
        Detail detail = job(() -> campaigns.link(marketer, id, new LinkRequest("PROMOTION", expired)));
        assertThat(detail.offers().getFirst().outsideWindow()).isTrue();
        assertThat(detail.warnings()).anyMatch(w -> w.contains("nằm ngoài thời gian chiến dịch"));
    }

    // ------------------------------------------------------------------ chi phí và phạm vi xem

    @Test
    void expensesAreRecordedInAnyStatusAndBudgetOverrunWarns() {
        UUID id = create(marketer, "Có ngân sách").campaign().id();
        Detail added = job(() -> campaigns.addExpense(marketer, id,
                new ExpenseRequest("ADS", new BigDecimal("3000000"), LocalDate.now(), "Facebook", null)));
        assertThat(added.expenses()).hasSize(1);
        assertThat(added.campaign().expenseTotal()).isEqualByComparingTo("3000000");
        assertThat(added.warnings()).isEmpty();

        UUID expenseId = added.expenses().getFirst().id();
        Detail over = job(() -> campaigns.updateExpense(marketer, id, expenseId,
                new ExpenseRequest("KOL", new BigDecimal("6000000"), LocalDate.now(), null, "Booking KOL")));
        assertThat(over.warnings()).anyMatch(w -> w.contains("vượt ngân sách"));
        assertStatus(() -> job(() -> campaigns.addExpense(marketer, id,
                new ExpenseRequest("TIKTOK", BigDecimal.TEN, LocalDate.now(), null, null))), HttpStatus.BAD_REQUEST);

        job(() -> campaigns.cancel(marketer, id, "Dừng chiến dịch"));
        assertThat(job(() -> campaigns.addExpense(marketer, id,
                new ExpenseRequest("PRINTING", new BigDecimal("500000"), LocalDate.now(), null, null))).expenses()).hasSize(2);
        assertThat(job(() -> campaigns.deleteExpense(marketer, id, expenseId)).expenses()).hasSize(1);
        assertStatus(() -> job(() -> campaigns.addExpense(storeManagerWithManage, id,
                new ExpenseRequest("ADS", BigDecimal.TEN, LocalDate.now(), null, null))), HttpStatus.FORBIDDEN);
    }

    @Test
    void storeStaffSeeOnlyApprovedCampaignsWithoutFinancials() {
        UUID draft = create(marketer, "Nháp " + tag).campaign().id();
        UUID live = create(marketer, "Đã duyệt " + tag).campaign().id();
        job(() -> campaigns.addExpense(marketer, live, new ExpenseRequest("ADS", BigDecimal.TEN, LocalDate.now(), null, null)));
        job(() -> campaigns.submit(marketer, live));
        job(() -> campaigns.approve(admin, live, null));

        assertStatus(() -> campaigns.detail(storeManager, draft), HttpStatus.NOT_FOUND);
        Detail seen = campaigns.detail(storeManager, live);
        assertThat(seen.campaign().plannedBudget()).isNull();
        assertThat(seen.campaign().expenseTotal()).isNull();
        assertThat(seen.expenses()).isEmpty();
        assertThat(seen.history()).isEmpty();
        assertThat(seen.actions().cancel()).isFalse();
        assertThat(seen.actions().viewFinancials()).isFalse();

        var listed = campaigns.list(storeManager, null, null, null, null, null, tag, PageRequest.of(0, 20)).getContent();
        assertThat(listed).extracting(MarketingCampaignDtos.Campaign::id).containsExactly(live);
        assertThat(campaigns.list(marketer, null, null, null, null, null, tag, PageRequest.of(0, 20)).getContent()).hasSize(2);
        assertThat(campaigns.list(marketer, null, "UPCOMING", null, null, null, tag, PageRequest.of(0, 20)).getContent())
                .extracting(MarketingCampaignDtos.Campaign::id).containsExactly(live);

        var lookups = campaigns.lookups(storeManager);
        assertThat(lookups.global()).isFalse();
        assertThat(lookups.canManage()).as("Phase 4: quản lý cửa hàng lập chiến dịch cho cửa hàng mình").isTrue();
        assertThat(lookups.storeId()).isEqualTo(store);
        assertThat(campaigns.lookups(marketer).canLinkOffers()).isTrue();
        assertThat(campaigns.lookups(admin).canApprove()).isTrue();
    }

    // ------------------------------------------------------------------ dữ liệu mẫu

    private Detail create(UUID actor, String name) {
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        return job(() -> campaigns.create(actor, new CampaignRequest(name, "REVENUE", "Mô tả", start, start.plusDays(10), null,
                new BigDecimal("5000000"), new BigDecimal("100000000"), 200, 150, null)));
    }

    private static CampaignRequest request(String name, LocalDateTime start, LocalDateTime end) {
        return new CampaignRequest(name, "RETENTION", null, start, end, null, null, null, null, null, null);
    }

    private UUID promotion(String type, String status) {
        UUID id = UUID.randomUUID();
        String code = ("ZMC" + tag + id.toString().substring(0, 6)).toUpperCase();
        jdbc.update("""
                insert into promotions(id, code, name, promotion_type, discount_type, discount_value, start_date, end_date, status,
                                       created_at)
                values (?, ?, ?, ?, 'PERCENT', 10, (now() at time zone 'UTC') - interval '1 day',
                        (now() at time zone 'UTC') + interval '30 day', ?, (now() at time zone 'UTC'))""",
                id, code, "Zmc " + code, type, status);
        return id;
    }

    private UUID flashSale(boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into flash_sales(id, name, starts_at, ends_at, active)
                values (?, ?, (now() at time zone 'UTC') + interval '2 day', (now() at time zone 'UTC') + interval '6 day', ?)""",
                id, "Zmc flash " + tag, active);
        return id;
    }

    private void employee(UUID id, String name, String role, UUID storeId) {
        jdbc.update("insert into users(id, username, full_name, active, locked) values (?, ?, ?, true, false)",
                id, "zmc-" + name + "-" + tag, "Zmc " + name);
        jdbc.update("insert into user_roles(user_id, role_id) select ?, id from roles where code = ?", id, role);
        if (storeId != null) jdbc.update("insert into store_staffs(user_id, store_id, active) values (?, ?, true)", id, storeId);
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_MARKETING_CAMPAIGN")) {
            return action.get();
        }
    }
}
