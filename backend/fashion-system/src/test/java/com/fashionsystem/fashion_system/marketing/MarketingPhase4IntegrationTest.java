package com.fashionsystem.fashion_system.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.AudienceCriteria;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.CampaignRequest;
import com.fashionsystem.fashion_system.marketing.MarketingCampaignDtos.DispatchRequest;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Phase 4: đồng ý nhận tin / hủy đăng ký, email marketing, tắt tin trong app, nhóm đối chứng, chiến dịch theo cửa hàng.
 * Job gửi dùng nhiều giao dịch riêng nên test commit dữ liệu thật và dọn ở {@link #cleanUp}; nhóm khách luôn lọc theo hạng
 * tạo riêng cho test nên không gửi gì tới khách thật. Email đi qua MailHelper giả.
 */
@SpringBootTest(properties = "marketing-dispatch.check-ms=3600000")
class MarketingPhase4IntegrationTest {
    @Autowired MarketingCampaignService campaigns;
    @Autowired MarketingDispatchService dispatches;
    @Autowired MarketingDispatchWorker worker;
    @Autowired MarketingDispatchScheduler scheduler;
    @Autowired MarketingConsentService consents;
    @Autowired MarketingReportService reports;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MarketingCampaignNotifier notifier;
    @MockitoBean MailHelper mail;

    final String tag = UUID.randomUUID().toString().substring(0, 8);
    final UUID admin = UUID.randomUUID();
    final UUID marketer = UUID.randomUUID();
    final UUID managerA = UUID.randomUUID();
    final UUID managerB = UUID.randomUUID();
    final UUID staffA = UUID.randomUUID();
    final UUID staffB = UUID.randomUUID();
    final UUID storeA = UUID.randomUUID();
    final UUID storeB = UUID.randomUUID();
    final UUID tier = UUID.randomUUID();
    final List<UUID> users = List.of(admin, marketer, managerA, managerB, staffA, staffB);
    final List<UUID> customers = new ArrayList<>();
    final List<UUID> campaignIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        store(storeA, "A");
        store(storeB, "B");
        employee(admin, "admin", "ADMIN", null);
        employee(marketer, "mkt", "MARKETING", null);
        employee(managerA, "man-a", "MANAGER", storeA);
        employee(managerB, "man-b", "MANAGER", storeB);
        employee(staffA, "staff-a", "STAFF", storeA);
        employee(staffB, "staff-b", "STAFF", storeB);
        jdbc.update("insert into customer_tiers(id, code, name, min_total_spent) values (?, ?, ?, 999999999)", tier, "ZM4" + tag, "Zm4 " + tag);
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
            jdbc.update("delete from orders where customer_id = ?", customer);
            jdbc.update("delete from customer_tier_assignments where customer_id = ?", customer);
            jdbc.update("delete from customer_accounts where customer_id = ?", customer);
            jdbc.update("delete from customers where id = ?", customer);
        }
        jdbc.update("delete from customer_tiers where id = ?", tier);
        for (UUID user : users) {
            jdbc.update("delete from notifications where recipient_id = ?", user);
            jdbc.update("delete from store_staffs where user_id = ?", user);
            jdbc.update("delete from user_roles where user_id = ?", user);
            jdbc.update("delete from users where id = ?", user);
        }
        jdbc.update("delete from stores where id in (?, ?)", storeA, storeB);
    }

    // ------------------------------------------------------------------ đồng ý nhận tin

    @Test
    void emailConsentIsOptInLoggedAndWithdrawableByLink() {
        UUID withEmail = customer("a", "zm4a-" + tag + "@example.com", false, null);
        UUID noEmail = customer("b", null, false, null);

        var initial = consents.preferences(withEmail);
        assertThat(initial.emailOptIn()).as("mặc định không gửi email khuyến mãi").isFalse();
        assertThat(initial.inAppEnabled()).isTrue();
        assertThat(initial.email()).startsWith("z***@");

        assertThat(job(() -> consents.update(withEmail, new MarketingConsentService.PreferencesRequest(true, true))).emailOptIn()).isTrue();
        assertThat(events(withEmail)).containsExactly("EMAIL:GRANTED:CUSTOMER_PROFILE");
        assertStatus(() -> job(() -> consents.update(noEmail, new MarketingConsentService.PreferencesRequest(true, true))), HttpStatus.CONFLICT);

        UUID token = token(withEmail);
        assertThat(consents.unsubscribeInfo(token).subscribed()).isTrue();
        assertThat(job(() -> consents.unsubscribe(token)).subscribed()).isFalse();
        job(() -> consents.unsubscribe(token)); // gọi lại không sao, không ghi thêm
        assertThat(consents.preferences(withEmail).emailOptIn()).isFalse();
        assertThat(events(withEmail)).containsExactly("EMAIL:GRANTED:CUSTOMER_PROFILE", "EMAIL:WITHDRAWN:UNSUBSCRIBE_LINK");
        assertStatus(() -> consents.unsubscribeInfo(UUID.randomUUID()), HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ email marketing và tắt tin trong app

    @Test
    void emailGoesOnlyToConsentedCustomersAndRechecksConsentAtSendTime() {
        UUID consented = customer("a", "zm4a-" + tag + "@example.com", true, null);
        UUID noConsent = customer("b", "zm4b-" + tag + "@example.com", true, null);
        UUID noEmail = customer("c", null, true, null);
        UUID latecomer = customer("d", "zm4d-" + tag + "@example.com", true, null);
        UUID optedOut = customer("e", null, true, null);
        consent(consented, true, true);
        consent(latecomer, true, true);
        consent(optedOut, false, false);

        UUID campaign = approvedCampaign(marketer, null, "EMAIL");
        UUID emailSend = onlyDispatch(campaign);
        makeDue(emailSend);
        job(() -> worker.startDue(LocalDateTime.now(), 24));
        assertThat(statuses(emailSend)).containsEntry(consented, "PENDING").containsEntry(latecomer, "PENDING")
                .containsEntry(noConsent, "SKIPPED").containsEntry(noEmail, "SKIPPED");
        assertThat(reason(emailSend, noConsent)).isEqualTo("NO_CONSENT");
        assertThat(reason(emailSend, noEmail)).isEqualTo("NO_EMAIL");

        // Khách hủy đăng ký sau khi danh sách đã chốt: lúc gửi phải bỏ qua.
        job(() -> consents.unsubscribe(token(latecomer)));
        run();
        assertThat(statuses(emailSend)).containsEntry(consented, "SENT").containsEntry(latecomer, "SKIPPED");
        assertThat(reason(emailSend, latecomer)).isEqualTo("NO_CONSENT");
        verify(mail).sendHtml(eq("zm4a-" + tag + "@example.com"), anyString(), contains("/unsubscribe?token=" + token(consented)));
        verify(mail, never()).sendHtml(eq("zm4d-" + tag + "@example.com"), anyString(), anyString());

        // Đợt in-app thêm sau khi chiến dịch đã duyệt: khách tắt tin bị bỏ qua; giới hạn tần suất tính theo từng kênh.
        UUID inApp = job(() -> dispatches.create(marketer, campaign, request("IN_APP", 0)));
        job(() -> { dispatches.approve(admin, campaign, inApp); return null; });
        makeDue(inApp);
        run();
        assertThat(reason(inApp, optedOut)).isEqualTo("OPTED_OUT");
        assertThat(statuses(inApp)).as("đã nhận email trong 24 giờ vẫn nhận tin in-app").containsEntry(consented, "SENT");
    }

    // ------------------------------------------------------------------ nhóm đối chứng

    @Test
    void holdoutGroupIsNotSentAndExperimentComparesBuyers() {
        for (int i = 0; i < 60; i++) customer("h" + i, null, true, null);
        UUID campaign = draftCampaign(marketer, null);
        job(() -> dispatches.create(marketer, campaign, request("IN_APP", 50)));
        job(() -> campaigns.submit(marketer, campaign));
        job(() -> campaigns.approve(admin, campaign, null));
        UUID send = onlyDispatch(campaign);
        makeDue(send);
        run();

        Map<UUID, String> statuses = statuses(send);
        List<UUID> treated = statuses.entrySet().stream().filter(e -> e.getValue().equals("SENT")).map(Map.Entry::getKey).toList();
        List<UUID> heldOut = statuses.entrySet().stream().filter(e -> e.getValue().equals("HOLDOUT")).map(Map.Entry::getKey).toList();
        assertThat(treated.size() + heldOut.size()).isEqualTo(60);
        assertThat(treated).hasSizeGreaterThan(10);
        assertThat(heldOut).hasSizeGreaterThan(10);
        Integer holdoutNotifications = jdbc.queryForObject("""
                select count(*) from notifications n join marketing_dispatch_recipients r on r.event_id = n.event_id
                where r.dispatch_id = ? and r.status = 'HOLDOUT'""", Integer.class, send);
        assertThat(holdoutNotifications).isZero();

        // Sau khi gửi: 5 người nhận và 1 người đối chứng mua hàng.
        treated.stream().limit(5).forEach(c -> order(c));
        order(heldOut.getFirst());
        var experiment = reports.performance(admin, campaign).experiments().getFirst();
        assertThat(experiment.treated()).isEqualTo(treated.size());
        assertThat(experiment.holdout()).isEqualTo(heldOut.size());
        assertThat(experiment.treatedBuyers()).isEqualTo(5);
        assertThat(experiment.holdoutBuyers()).isEqualTo(1);
        assertThat(experiment.upliftPoints()).isNotNull();
    }

    // ------------------------------------------------------------------ chiến dịch theo cửa hàng

    @Test
    void storeManagersRunCampaignsOnlyForTheirOwnStore() {
        UUID storeCustomer = customer("s", null, true, storeA);
        customer("w", null, true, null);

        var lookups = campaigns.lookups(managerA);
        assertThat(lookups.canManage()).isTrue();
        assertThat(lookups.storeId()).isEqualTo(storeA);
        assertThat(lookups.stores()).isEmpty();

        UUID campaign = draftCampaign(managerA, null);
        assertThat(campaigns.detail(managerA, campaign).campaign().storeId()).isEqualTo(storeA);
        assertThat(campaigns.detail(managerA, campaign).actions().edit()).isTrue();
        assertStatus(() -> job(() -> campaigns.create(managerA, campaignRequest("Sai cửa hàng", storeB))), HttpStatus.FORBIDDEN);
        assertStatus(() -> campaigns.detail(managerB, campaign), HttpStatus.NOT_FOUND);
        assertStatus(() -> campaigns.detail(staffA, campaign), HttpStatus.NOT_FOUND); // còn nháp
        assertStatus(() -> job(() -> campaigns.submit(managerB, campaign)), HttpStatus.FORBIDDEN);

        // Nhóm khách của chiến dịch cửa hàng chỉ gồm khách của cửa hàng đó; quản lý cửa hàng phải chọn chiến dịch để xem trước.
        assertStatus(() -> dispatches.preview(managerA, new MarketingCampaignDtos.PreviewRequest(criteria(), "IN_APP", null)),
                HttpStatus.BAD_REQUEST);
        var preview = dispatches.preview(managerA, new MarketingCampaignDtos.PreviewRequest(criteria(), "IN_APP", campaign));
        assertThat(preview.matched()).isEqualTo(1);
        assertThat(preview.sample()).extracting(MarketingCampaignDtos.PreviewCustomer::customerCode).containsExactly("ZM4" + tag + "s");

        job(() -> campaigns.submit(managerA, campaign));
        assertStatus(() -> job(() -> campaigns.approve(managerA, campaign, null)), HttpStatus.FORBIDDEN);
        job(() -> campaigns.approve(admin, campaign, null));

        // Đã duyệt: nhân viên cửa hàng A thấy, không thấy số liệu; cửa hàng B không thấy.
        assertThat(campaigns.list(staffA, null, null, null, null, null, tag, PageRequest.of(0, 20)).getContent())
                .extracting(MarketingCampaignDtos.Campaign::id).containsExactly(campaign);
        assertThat(campaigns.detail(staffA, campaign).campaign().plannedBudget()).isNull();
        assertThat(campaigns.list(staffB, null, null, null, null, null, tag, PageRequest.of(0, 20)).getContent()).isEmpty();
        assertThat(reports.performance(managerA, campaign).storeId()).isEqualTo(storeA);
        assertStatus(() -> reports.performance(managerB, campaign), HttpStatus.FORBIDDEN);
        assertStatus(() -> reports.performance(staffA, campaign), HttpStatus.FORBIDDEN);
        assertThat(storeCustomer).isNotNull();
    }

    // ------------------------------------------------------------------ dữ liệu mẫu

    private UUID draftCampaign(UUID actor, UUID store) {
        UUID id = job(() -> campaigns.create(actor, campaignRequest("Zm4 " + tag, store))).campaign().id();
        campaignIds.add(id);
        return id;
    }

    private CampaignRequest campaignRequest(String name, UUID store) {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        return new CampaignRequest(name, "RETENTION", null, start, start.plusDays(10), null, java.math.BigDecimal.valueOf(1_000_000),
                null, null, null, store);
    }

    private UUID approvedCampaign(UUID actor, UUID store, String channel) {
        UUID campaign = draftCampaign(actor, store);
        job(() -> dispatches.create(actor, campaign, request(channel, 0)));
        job(() -> campaigns.submit(actor, campaign));
        job(() -> campaigns.approve(admin, campaign, null));
        return campaign;
    }

    private AudienceCriteria criteria() {
        return new AudienceCriteria(List.of(tier), null, null);
    }

    private DispatchRequest request(String channel, int holdout) {
        return new DispatchRequest(criteria(), "Ưu đãi riêng cho bạn", "Giảm 20% cuối tuần này", "/promotions", channel,
                LocalDateTime.now().plusHours(1), holdout);
    }

    private UUID onlyDispatch(UUID campaign) {
        return jdbc.queryForObject("select id from marketing_dispatches where campaign_id = ? order by created_at limit 1", UUID.class, campaign);
    }

    private void makeDue(UUID dispatch) {
        jdbc.update("update marketing_dispatches set scheduled_at = (now() at time zone 'UTC') - interval '1 minute' where id = ?", dispatch);
    }

    private void run() {
        job(() -> scheduler.runOnce());
    }

    private Map<UUID, String> statuses(UUID dispatch) {
        return jdbc.queryForList("select customer_id, status from marketing_dispatch_recipients where dispatch_id = ?", dispatch).stream()
                .collect(Collectors.toMap(r -> (UUID) r.get("customer_id"), r -> (String) r.get("status")));
    }

    private String reason(UUID dispatch, UUID customer) {
        return jdbc.queryForObject("select skip_reason from marketing_dispatch_recipients where dispatch_id = ? and customer_id = ?",
                String.class, dispatch, customer);
    }

    private UUID token(UUID customer) {
        return jdbc.queryForObject("select unsubscribe_token from marketing_customer_preferences where customer_id = ?", UUID.class, customer);
    }

    private List<String> events(UUID customer) {
        return jdbc.queryForList("select channel || ':' || action || ':' || source from marketing_consent_events where customer_id = ? order by created_at",
                String.class, customer);
    }

    private void consent(UUID customer, boolean email, boolean inApp) {
        job(() -> consents.update(customer, new MarketingConsentService.PreferencesRequest(email, inApp)));
    }

    private void order(UUID customer) {
        jdbc.update("""
                insert into orders(order_code, order_type, status, total_amount, shipping_fee, customer_id, created_at, active)
                values (?, 'OFFLINE', 'COMPLETED', 100000, 0, ?, (now() at time zone 'UTC'), true)""",
                "ZM4" + UUID.randomUUID().toString().substring(0, 12), customer);
    }

    private UUID customer(String suffix, String email, boolean account, UUID originStore) {
        UUID id = UUID.randomUUID();
        customers.add(id);
        jdbc.update("""
                insert into customers(id, customer_code, full_name, phone, email, active, membership_status, source, origin_store_id, created_at)
                values (?, ?, ?, ?, ?, true, 'MEMBER', ?, ?, (now() at time zone 'UTC'))""",
                id, "ZM4" + tag + suffix, "Zm4 khách " + suffix, "09" + String.format("%08d", Math.floorMod(id.hashCode(), 100_000_000)),
                email, originStore == null ? "WEBSITE" : "STORE", originStore);
        jdbc.update("insert into customer_tier_assignments(id, customer_id, tier_id, assigned_at) values (?, ?, ?, (now() at time zone 'UTC'))",
                UUID.randomUUID(), id, tier);
        if (account) {
            jdbc.update("""
                    insert into customer_accounts(id, created_at, customer_id, failed_login_attempts, updated_at, username)
                    values (?, (now() at time zone 'UTC'), ?, 0, (now() at time zone 'UTC'), ?)""",
                    UUID.randomUUID(), id, ("zm4" + tag + suffix).substring(0, Math.min(50, ("zm4" + tag + suffix).length())));
        }
        return id;
    }

    private void store(UUID id, String suffix) {
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, ?, true, ?, ?)",
                id, "ZM4" + suffix + tag, "Zm4 Store " + suffix, 10 + Math.random(), 100 + Math.random());
    }

    private void employee(UUID id, String name, String role, UUID store) {
        jdbc.update("insert into users(id, username, full_name, active, locked) values (?, ?, ?, true, false)",
                id, "zm4-" + name + "-" + tag, "Zm4 " + name);
        jdbc.update("insert into user_roles(user_id, role_id) select ?, id from roles where code = ?", id, role);
        if (store != null) jdbc.update("insert into store_staffs(user_id, store_id, active) values (?, ?, true)", id, store);
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_MARKETING_PHASE4")) {
            return action.get();
        }
    }
}
