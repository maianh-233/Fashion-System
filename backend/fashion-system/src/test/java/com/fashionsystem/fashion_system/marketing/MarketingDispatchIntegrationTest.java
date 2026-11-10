package com.fashionsystem.fashion_system.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.promotion.VoucherCampaignDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.AudienceCriteria;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.CampaignRequest;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.Dispatch;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.DispatchRequest;
import com.fashionsystem.fashion_system.notification.NotificationPersistenceService;
import com.fashionsystem.fashion_system.service.VoucherCampaignService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Job gửi chạy nhiều giao dịch riêng và ghi thông báo bằng REQUIRES_NEW, nên test này KHÔNG bọc trong một giao dịch:
 * dữ liệu được commit thật và dọn ở {@link #cleanUp}. Scheduler tự động bị đẩy chu kỳ lên 1 giờ để không chen vào test;
 * test gọi {@link MarketingDispatchScheduler#runOnce} trực tiếp. Nhóm khách luôn lọc theo một hạng tạo riêng cho test
 * nên không gửi gì tới khách thật trong cơ sở dữ liệu.
 */
@SpringBootTest(properties = "marketing-dispatch.check-ms=3600000")
class MarketingDispatchIntegrationTest {
    @Autowired MarketingCampaignService campaigns;
    @Autowired MarketingDispatchService dispatches;
    @Autowired MarketingDispatchWorker worker;
    @Autowired MarketingDispatchScheduler scheduler;
    @Autowired VoucherCampaignService voucherCampaigns;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MarketingCampaignNotifier notifier;
    @MockitoSpyBean NotificationPersistenceService persistence;

    final String tag = UUID.randomUUID().toString().substring(0, 8);
    final UUID admin = UUID.randomUUID();
    final UUID admin2 = UUID.randomUUID();
    final UUID marketer = UUID.randomUUID();
    final UUID tier = UUID.randomUUID();
    final UUID withAccountA = UUID.randomUUID();
    final UUID withAccountB = UUID.randomUUID();
    final UUID noAccount = UUID.randomUUID();
    final UUID inactive = UUID.randomUUID();
    final List<UUID> campaignIds = new ArrayList<>();
    final List<UUID> users = List.of(admin, admin2, marketer);
    final List<UUID> customers = List.of(withAccountA, withAccountB, noAccount, inactive);

    @BeforeEach
    void setUp() {
        employee(admin, "admin", "ADMIN");
        employee(admin2, "admin2", "ADMIN");
        employee(marketer, "mkt", "MARKETING");
        jdbc.update("insert into customer_tiers(id, code, name, min_total_spent) values (?, ?, ?, 999999999)", tier, "ZMD" + tag, "Zmd tier " + tag);
        customer(withAccountA, "a", true, true);
        customer(withAccountB, "b", true, true);
        customer(noAccount, "c", false, true);
        customer(inactive, "d", true, false);
    }

    @AfterEach
    void cleanUp() {
        for (UUID campaign : campaignIds) {
            jdbc.update("""
                    delete from notifications where reference_type = 'MARKETING_DISPATCH'
                      and reference_id in (select id from marketing_dispatches where campaign_id = ?)""", campaign);
            jdbc.update("delete from marketing_dispatch_recipients where dispatch_id in (select id from marketing_dispatches where campaign_id = ?)", campaign);
            jdbc.update("delete from marketing_dispatches where campaign_id = ?", campaign);
            jdbc.update("delete from marketing_campaigns where id = ?", campaign);
        }
        for (UUID customer : customers) {
            jdbc.update("delete from customer_tier_assignments where customer_id = ?", customer);
            jdbc.update("delete from customer_accounts where customer_id = ?", customer);
            jdbc.update("delete from customers where id = ?", customer);
        }
        jdbc.update("delete from customer_tiers where id = ?", tier);
        for (UUID user : users) {
            jdbc.update("delete from user_roles where user_id = ?", user);
            jdbc.update("delete from users where id = ?", user);
        }
    }

    // ------------------------------------------------------------------ gửi, chốt danh sách, chống gửi trùng

    @Test
    void approvedDispatchSnapshotsAudienceSendsOnceAndRespectsFrequencyCap() {
        UUID campaign = approvedCampaignWithDraftDispatch();
        Dispatch first = onlyDispatch(campaign);
        assertThat(first.status()).isEqualTo("SCHEDULED");

        makeDue(first.id());
        run();

        Map<UUID, String> statuses = recipientStatuses(first.id());
        assertThat(statuses).containsEntry(withAccountA, "SENT").containsEntry(withAccountB, "SENT").containsEntry(noAccount, "SKIPPED")
                .doesNotContainKey(inactive);
        assertThat(skipReason(first.id(), noAccount)).isEqualTo("NO_ACCOUNT");
        assertThat(notificationCount(first.id(), withAccountA)).isEqualTo(1);
        Dispatch done = onlyDispatch(campaign);
        assertThat(done.status()).isEqualTo("COMPLETED");
        assertThat(done.sent()).isEqualTo(2);
        assertThat(done.skipped()).isEqualTo(1);
        assertThat(done.audienceCount()).isEqualTo(3);

        // Sự cố sau khi ghi thông báo nhưng trước khi đánh dấu SENT: dòng còn SENDING với lease đã hết hạn.
        jdbc.update("update marketing_dispatches set status = 'SENDING' where id = ?", first.id());
        jdbc.update("""
                update marketing_dispatch_recipients set status = 'SENDING', sent_at = null,
                       lease_until = (now() at time zone 'UTC') - interval '1 minute'
                where dispatch_id = ? and customer_id = ?""", first.id(), withAccountA);
        run();
        assertThat(recipientStatuses(first.id())).containsEntry(withAccountA, "SENT");
        assertThat(notificationCount(first.id(), withAccountA)).as("event_id cố định chặn thông báo thứ hai").isEqualTo(1);
        assertThat(onlyDispatch(campaign).status()).isEqualTo("COMPLETED");

        // Đợt thêm sau khi chiến dịch đã duyệt cần duyệt riêng; khách vừa nhận tin bị bỏ qua do giới hạn tần suất.
        UUID second = job(() -> dispatches.create(marketer, campaign, request(LocalDateTime.now().plusHours(2))));
        assertThat(dispatch(campaign, second).status()).isEqualTo("PENDING_APPROVAL");
        assertStatus(() -> job(() -> { dispatches.approve(marketer, campaign, second); return null; }), HttpStatus.FORBIDDEN);
        job(() -> { dispatches.approve(admin, campaign, second); return null; });
        makeDue(second);
        run();
        assertThat(skipReason(second, withAccountA)).isEqualTo("FREQUENCY_CAP");
        assertThat(dispatch(campaign, second).status()).isEqualTo("COMPLETED");
        assertThat(dispatch(campaign, second).sent()).isZero();
    }

    @Test
    void transientFailuresBackOffThenFailAndRetryFailedResends() {
        UUID campaign = approvedCampaignWithDraftDispatch();
        UUID id = onlyDispatch(campaign).id();
        makeDue(id);
        doThrow(new TransientDataAccessResourceException("database restarting")).when(persistence).persist(any());

        run();
        assertThat(recipientStatuses(id)).containsEntry(withAccountA, "PENDING");
        assertThat(jdbc.queryForObject("select attempt_count from marketing_dispatch_recipients where dispatch_id = ? and customer_id = ?",
                Integer.class, id, withAccountA)).isEqualTo(1);
        // Chưa tới giờ thử lại thì lượt chạy kế tiếp không gửi.
        run();
        assertThat(jdbc.queryForObject("select attempt_count from marketing_dispatch_recipients where dispatch_id = ? and customer_id = ?",
                Integer.class, id, withAccountA)).isEqualTo(1);

        for (int attempt = 2; attempt <= 4; attempt++) {
            jdbc.update("update marketing_dispatch_recipients set next_attempt_at = (now() at time zone 'UTC') - interval '1 second'"
                    + " where dispatch_id = ? and status = 'PENDING'", id);
            run();
        }
        assertThat(recipientStatuses(id)).containsEntry(withAccountA, "FAILED").containsEntry(withAccountB, "FAILED");
        Dispatch failed = onlyDispatch(campaign);
        assertThat(failed.status()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(failed.failed()).isEqualTo(2);
        assertThat(failed.actions().retry()).isTrue();
        assertThat(jdbc.queryForObject("select last_error_code from marketing_dispatch_recipients where dispatch_id = ? and customer_id = ?",
                String.class, id, withAccountA)).isEqualTo("TransientDataAccessResourceException");

        doCallRealMethod().when(persistence).persist(any());
        job(() -> { dispatches.retryFailed(marketer, campaign, id); return null; });
        run();
        assertThat(recipientStatuses(id)).containsEntry(withAccountA, "SENT").containsEntry(withAccountB, "SENT");
        assertThat(onlyDispatch(campaign).status()).isEqualTo("COMPLETED");
    }

    @Test
    void cancellingCampaignStopsSendingAndReopenNeedsNoStartedDispatch() {
        UUID campaign = approvedCampaignWithDraftDispatch();
        UUID id = onlyDispatch(campaign).id();

        // Đã duyệt nhưng chưa gửi: mở lại được, đợt quay về Nháp.
        job(() -> campaigns.reopen(marketer, campaign, "Đổi nội dung"));
        assertThat(onlyDispatch(campaign).status()).isEqualTo("DRAFT");
        job(() -> campaigns.submit(marketer, campaign));
        job(() -> campaigns.approve(admin, campaign, null));

        // Chốt danh sách rồi dừng trước khi gửi lô nào.
        makeDue(id);
        job(() -> worker.startDue(LocalDateTime.now(), 24));
        assertThat(onlyDispatch(campaign).status()).isEqualTo("SENDING");
        assertStatus(() -> job(() -> campaigns.reopen(marketer, campaign, "Muốn sửa")), HttpStatus.CONFLICT);

        job(() -> campaigns.cancel(marketer, campaign, "Dừng khẩn cấp"));
        Dispatch cancelled = onlyDispatch(campaign);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(recipientStatuses(id)).containsEntry(withAccountA, "SKIPPED");
        assertThat(skipReason(id, withAccountA)).isEqualTo("CANCELLED");
        run();
        assertThat(notificationCount(id, withAccountA)).isZero();
    }

    @Test
    void dispatchValidationAndApprovalRules() {
        UUID campaign = draftCampaign();
        LocalDateTime ok = LocalDateTime.now().plusHours(3);
        assertStatus(() -> job(() -> dispatches.create(marketer, campaign, new DispatchRequest(criteria(), "Tiêu đề", "Nội dung",
                null, "SMS", ok, 0))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> job(() -> dispatches.create(marketer, campaign, new DispatchRequest(criteria(), "Tiêu đề", "Nội dung",
                "/admin/orders", null, ok, 0))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> job(() -> dispatches.create(marketer, campaign, request(LocalDateTime.now().plusDays(30)))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> job(() -> dispatches.create(marketer, campaign, request(LocalDateTime.now().minusMinutes(5)))), HttpStatus.BAD_REQUEST);

        UUID id = job(() -> dispatches.create(marketer, campaign, request(ok)));
        assertThat(dispatch(campaign, id).status()).isEqualTo("DRAFT");
        // Giờ gửi của đợt Nháp đã qua → không duyệt được chiến dịch.
        jdbc.update("update marketing_dispatches set scheduled_at = (now() at time zone 'UTC') - interval '1 hour' where id = ?", id);
        job(() -> campaigns.submit(marketer, campaign));
        assertStatus(() -> job(() -> campaigns.approve(admin, campaign, null)), HttpStatus.CONFLICT);
        assertThat(campaigns.detail(admin, campaign).campaign().status()).isEqualTo("PENDING_APPROVAL");

        // Người tạo đợt bổ sung không tự duyệt được.
        job(() -> campaigns.reject(admin, campaign, "Sửa giờ gửi"));
        job(() -> { dispatches.update(marketer, campaign, id, request(ok)); return null; });
        job(() -> campaigns.submit(marketer, campaign));
        job(() -> campaigns.approve(admin, campaign, null));
        UUID extra = job(() -> dispatches.create(admin, campaign, request(ok.plusHours(1))));
        assertStatus(() -> job(() -> { dispatches.approve(admin, campaign, extra); return null; }), HttpStatus.FORBIDDEN);
        job(() -> { dispatches.reject(admin2, campaign, extra, "Trùng nội dung"); return null; });
        assertThat(dispatch(campaign, extra).status()).isEqualTo("CANCELLED");
    }

    @Test
    void previewMatchesVoucherCampaignSegment() {
        var preview = dispatches.preview(marketer, new MarketingCampaignDtos.PreviewRequest(criteria(), null, null));
        assertThat(preview.matched()).isEqualTo(3);
        assertThat(preview.reachable()).isEqualTo(2);
        assertThat(preview.afterFrequencyCap()).isEqualTo(2);
        assertThat(preview.sample()).allSatisfy(c -> assertThat(c.phone()).contains("****"));
        // Test hợp đồng: cùng tiêu chí thì đếm ra cùng nhóm khách với chiến dịch voucher.
        long voucherCount = voucherCampaigns.preview(new VoucherCampaignDtos.Criteria(List.of(tier), null, null, false)).count();
        assertThat(preview.matched()).isEqualTo(voucherCount);
    }

    // ------------------------------------------------------------------ dữ liệu mẫu

    private UUID draftCampaign() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        UUID id = job(() -> campaigns.create(marketer, new CampaignRequest("Zmd " + tag, "RETENTION", null, start, start.plusDays(10),
                null, null, null, null, null, null))).campaign().id();
        campaignIds.add(id);
        return id;
    }

    private UUID approvedCampaignWithDraftDispatch() {
        UUID campaign = draftCampaign();
        job(() -> dispatches.create(marketer, campaign, request(LocalDateTime.now().plusHours(1))));
        job(() -> campaigns.submit(marketer, campaign));
        job(() -> campaigns.approve(admin, campaign, null));
        return campaign;
    }

    private AudienceCriteria criteria() {
        return new AudienceCriteria(List.of(tier), null, null);
    }

    private DispatchRequest request(LocalDateTime at) {
        return new DispatchRequest(criteria(), "Ưu đãi riêng cho bạn", "Giảm 20% cuối tuần này", "/promotions", null, at, 0);
    }

    private Dispatch onlyDispatch(UUID campaign) {
        List<Dispatch> list = campaigns.detail(admin, campaign).dispatches();
        assertThat(list).isNotEmpty();
        return list.getFirst();
    }

    private Dispatch dispatch(UUID campaign, UUID id) {
        return campaigns.detail(admin, campaign).dispatches().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }

    private void makeDue(UUID dispatch) {
        jdbc.update("update marketing_dispatches set scheduled_at = (now() at time zone 'UTC') - interval '1 minute' where id = ?", dispatch);
    }

    private void run() {
        job(() -> scheduler.runOnce());
    }

    private Map<UUID, String> recipientStatuses(UUID dispatch) {
        return jdbc.queryForList("select customer_id, status from marketing_dispatch_recipients where dispatch_id = ?", dispatch).stream()
                .collect(Collectors.toMap(r -> (UUID) r.get("customer_id"), r -> (String) r.get("status")));
    }

    private String skipReason(UUID dispatch, UUID customer) {
        return jdbc.queryForObject("select skip_reason from marketing_dispatch_recipients where dispatch_id = ? and customer_id = ?",
                String.class, dispatch, customer);
    }

    private int notificationCount(UUID dispatch, UUID customer) {
        Integer count = jdbc.queryForObject("""
                select count(*) from notifications where reference_type = 'MARKETING_DISPATCH' and reference_id = ?
                  and recipient_type = 'CUSTOMER' and recipient_id = ?""", Integer.class, dispatch, customer);
        return count == null ? 0 : count;
    }

    private void employee(UUID id, String name, String role) {
        jdbc.update("insert into users(id, username, full_name, active, locked) values (?, ?, ?, true, false)",
                id, "zmd-" + name + "-" + tag, "Zmd " + name);
        jdbc.update("insert into user_roles(user_id, role_id) select ?, id from roles where code = ?", id, role);
    }

    private void customer(UUID id, String suffix, boolean account, boolean active) {
        jdbc.update("""
                insert into customers(id, customer_code, full_name, phone, active, membership_status, source, created_at)
                values (?, ?, ?, ?, ?, 'MEMBER', 'WEBSITE', (now() at time zone 'UTC'))""",
                id, "ZMD" + tag + suffix, "Zmd khách " + suffix, "09" + String.format("%08d", Math.floorMod(id.hashCode(), 100_000_000)), active);
        jdbc.update("insert into customer_tier_assignments(id, customer_id, tier_id, assigned_at) values (?, ?, ?, (now() at time zone 'UTC'))",
                UUID.randomUUID(), id, tier);
        if (account) {
            jdbc.update("""
                    insert into customer_accounts(id, created_at, customer_id, failed_login_attempts, updated_at, username)
                    values (?, (now() at time zone 'UTC'), ?, 0, (now() at time zone 'UTC'), ?)""",
                    UUID.randomUUID(), id, "zmd" + tag + suffix);
        }
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_MARKETING_DISPATCH")) {
            return action.get();
        }
    }
}
