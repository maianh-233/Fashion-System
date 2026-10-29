package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.CustomerPromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Voucher sinh nhật trên cơ sở dữ liệu thật; dữ liệu do test tạo và rollback sau mỗi test. */
@SpringBootTest
@Transactional
class BirthdayVoucherIntegrationTest {
    @Autowired BirthdayVoucherService birthdays;
    @Autowired OrderVoucherService vouchers;
    @Autowired OrderCartPricer pricer;
    @Autowired CustomerPromotionRepository customerPromotions;
    @Autowired PromotionRepository promotions;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @MockitoBean NotificationPublisher notifications;
    @MockitoBean MailHelper mail;

    LocalDateTime now;
    final UUID promotion = UUID.randomUUID();
    final UUID birthdayNow = UUID.randomUUID();
    final UUID birthdayLater = UUID.randomUUID();
    final UUID inactive = UUID.randomUUID();
    String code;

    @BeforeEach
    void setUp() {
        // Tính ở đây, không ở trường: instance test được tạo trước khi Spring context khởi động xong.
        now = PromotionService.now();
        String tag = promotion.toString().substring(0, 8);
        code = "ZQBD" + tag.toUpperCase();
        // Mẫu mới nhất: bắt đầu sau mọi mẫu BIRTHDAY khác có thể có sẵn trong DB.
        jdbc.update("""
                insert into promotions(id, code, name, discount_type, discount_value, start_date, end_date, min_order_value,
                                       max_discount, promotion_type, status)
                values (?, ?, 'Zqbd sinh nhật', 'PERCENT', 20, now() at time zone 'UTC' - interval '1 minute',
                        now() at time zone 'UTC' + interval '1 year', 300000, 100000, 'BIRTHDAY', 'ACTIVE')""", promotion, code);
        LocalDate thisMonth = LocalDate.of(1995, now.getMonthValue(), 1);
        customer(birthdayNow, tag + "a", thisMonth, true, "zqbd-" + tag + "@example.com");
        customer(birthdayLater, tag + "b", thisMonth.plusMonths(1), true, null);
        customer(inactive, tag + "c", thisMonth, false, null);
    }

    @Test
    void issuesOneVoucherForTheWholeBirthMonthAndNotifiesAfterCommit() {
        birthdays.issueDue(now);
        birthdays.issueDue(now);

        List<CustomerPromotion> issued = issuedTo(birthdayNow);
        assertThat(issued).singleElement().satisfies(voucher -> {
            YearMonth month = YearMonth.from(now);
            assertThat(voucher.getPromotionId()).isEqualTo(promotion);
            assertThat(voucher.getSource()).isEqualTo(CustomerPromotion.SOURCE_BIRTHDAY);
            assertThat(voucher.getPointsSpent()).isZero();
            assertThat(voucher.getBirthdayYear()).isEqualTo(month.getYear());
            assertThat(voucher.getValidFrom()).isEqualTo(month.atDay(1).atStartOfDay());
            assertThat(voucher.getExpiresAt()).isEqualTo(month.plusMonths(1).atDay(1).atStartOfDay());
        });
        assertThat(issuedTo(birthdayLater)).isEmpty();
        assertThat(issuedTo(inactive)).isEmpty();

        verifyNoInteractions(notifications, mail);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        // Gửi trên luồng nền: chờ thông báo của khách test (khách có sẵn trong DB cũng có thể được gửi cùng lúc).
        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notifications, org.mockito.Mockito.timeout(5000)).publish(org.mockito.ArgumentMatchers.argThat(
                n -> n.recipientIdentities().contains(RealtimeIdentity.customer(birthdayNow))));
        verify(notifications, org.mockito.Mockito.atLeastOnce()).publish(sent.capture());
        assertThat(sent.getAllValues()).filteredOn(n -> n.recipientIdentities().contains(RealtimeIdentity.customer(birthdayNow)))
                .singleElement().satisfies(n -> {
                    assertThat(n.type()).isEqualTo(NotificationType.BIRTHDAY_VOUCHER);
                    assertThat(n.message()).contains(code).contains("giảm 20% (tối đa 100.000đ) cho đơn từ 300.000đ");
                });
        verify(mail, org.mockito.Mockito.timeout(5000)).sendText(eq("zqbd-" + promotion.toString().substring(0, 8) + "@example.com"), anyString(), contains(code));
    }

    @Test
    void ownedBirthdayVoucherIsUsableOnlyByItsOwnerAndOnlyUntilItExpires() {
        birthdays.issueDue(now);
        Promotion template = promotions.findById(promotion).orElseThrow();
        var targets = vouchers.targets(List.of(promotion));

        assertThat(vouchers.ineligibleReason(template, vouchers.customer(birthdayNow), targets, now)).isNull();
        assertThat(vouchers.listable(vouchers.customer(birthdayNow), now)).extracting(Promotion::getId).contains(promotion);
        assertThat(vouchers.ineligibleReason(template, vouchers.customer(birthdayLater), targets, now))
                .isEqualTo("Khách không có voucher sinh nhật còn hạn");
        assertThat(vouchers.ineligibleReason(template, OrderVoucherService.CustomerContext.walkIn(), targets, now))
                .isEqualTo("Voucher sinh nhật cần chọn khách hàng");

        jdbc.update("update customer_promotions set valid_from = valid_from - interval '2 months', "
                + "expires_at = now() at time zone 'UTC' - interval '1 minute' where customer_id = ?", birthdayNow);
        entityManager.flush();
        entityManager.clear();
        assertThat(vouchers.customer(birthdayNow).ownedVoucherIds()).doesNotContain(promotion);
    }

    @Test
    void usingTheVoucherOnAnOrderConsumesIt() {
        birthdays.issueDue(now);
        UUID order = UUID.randomUUID();
        jdbc.update("insert into orders(id, order_code, order_type, status, customer_id) values (?, ?, 'ONLINE', 'PENDING', ?)",
                order, code, birthdayNow);
        pricer.recordApplied(java.util.Map.of(promotions.findById(promotion).orElseThrow(), new java.math.BigDecimal("60000")),
                order, birthdayNow, now);

        assertThat(issuedTo(birthdayNow)).singleElement().satisfies(voucher -> assertThat(voucher.getUsedOrderId()).isEqualTo(order));
        assertThat(vouchers.customer(birthdayNow).ownedVoucherIds()).doesNotContain(promotion);
        assertThat(jdbc.queryForObject("select count(*) from promotion_usages where promotion_id = ?", Integer.class, promotion)).isZero();
    }

    @Test
    void tierRestrictedTemplateOnlyReachesCustomersInThatTier() {
        UUID tier = UUID.randomUUID();
        jdbc.update("insert into customer_tiers(id, code, name, min_total_spent) values (?, ?, 'Zqbd Vàng', 987654321)",
                tier, "ZQBD" + promotion.toString().substring(0, 6));
        jdbc.update("insert into promotion_tiers(promotion_id, tier_id) values (?, ?)", promotion, tier);
        birthdays.issueDue(now);
        assertThat(issuedTo(birthdayNow)).isEmpty();

        jdbc.update("insert into customer_tier_assignments(id, customer_id, tier_id, assigned_at) values (?, ?, ?, now())",
                UUID.randomUUID(), birthdayNow, tier);
        birthdays.issueDue(now);
        assertThat(issuedTo(birthdayNow)).hasSize(1);
    }

    @Test
    void nothingIsIssuedWithoutARunningTemplate() {
        jdbc.update("update promotions set status = 'INACTIVE' where promotion_type = 'BIRTHDAY'");
        assertThat(birthdays.issueDue(now)).isZero();
    }

    private List<CustomerPromotion> issuedTo(UUID customer) {
        entityManager.flush();
        entityManager.clear();
        return customerPromotions.findAllByCustomerId(customer, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    private void customer(UUID id, String tag, LocalDate birthday, boolean active, String email) {
        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name, date_of_birth, active, email) "
                + "values (?, ?, 'MEMBER', 'WEBSITE', 'Zqbd Khách', ?, ?, ?)", id, "ZQBD-" + tag, birthday, active, email);
    }
}
