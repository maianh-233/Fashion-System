package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.support.BaselineCatalogFixture;
import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tích điểm theo đơn và trừ khi hoàn tiền trên cơ sở dữ liệu thật; dữ liệu mẫu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class LoyaltyEarningIntegrationTest {
    @Autowired LoyaltyEarningService loyalty;
    @Autowired OrderStatusService statusService;
    @Autowired RefundService refundService;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    final UUID customer = UUID.randomUUID();
    final UUID silver = UUID.randomUUID();
    UUID store;

    @BeforeEach
    void setUp() {
        BaselineCatalogFixture.ensure(jdbc);
        jdbc.update("insert into customers(id, customer_code, membership_status, source) values (?, ?, 'MEMBER', 'WEBSITE')",
                customer, "ZQXW-" + customer.toString().substring(0, 8));
        jdbc.update("insert into customer_tiers(id, code, name, min_total_spent, discount_percent) values (?, 'ZQXW_SILVER', 'Zqxw Bạc', 500000, 0)", silver);
        jdbc.update("insert into customer_tiers(id, code, name, min_total_spent, discount_percent) values (?, 'ZQXW_GOLD', 'Zqxw Vàng', 900000000, 0)", UUID.randomUUID());
        store = jdbc.queryForObject("select id from stores where active = true order by code limit 1", UUID.class);
    }

    private UUID order(String type, String status, UUID customerId, UUID storeId, UUID parent, String total, String discount) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into orders(id, order_code, order_type, status, active, store_id, customer_id, parent_order_id, "
                        + "subtotal, discount_total, shipping_fee, total_amount, payment_status) "
                        + "values (?, ?, ?, ?, true, ?, ?, ?, ?, ?, 0, ?, 'PAID')",
                id, "ZQXW-" + id.toString().substring(0, 12), type, status, storeId, customerId, parent,
                new BigDecimal(total), new BigDecimal(discount), new BigDecimal(total));
        return id;
    }

    private void earn(UUID orderId) {
        loyalty.earnForCompletedOrder(orders.findById(orderId).orElseThrow());
        sync();
    }

    /** Ghi xuống DB và bỏ cache JPA để đọc bằng JDBC (và sửa bằng JDBC) thấy cùng dữ liệu. */
    private void sync() {
        entityManager.flush();
        entityManager.clear();
    }

    private Map<String, Object> account() {
        sync();
        return jdbc.queryForMap("select points_balance, total_spent from loyalty_accounts where customer_id = ?", customer);
    }

    private String currentTier() {
        sync();
        return jdbc.queryForObject("select t.code from customer_tier_assignments a join customer_tiers t on t.id = a.tier_id "
                + "where a.customer_id = ? and a.expires_at is null", String.class, customer);
    }

    @Test
    void completedStoreOrderEarnsOnceAndUpgradesTier() {
        UUID pos = order("OFFLINE", "COMPLETED", customer, store, null, "1234567", "0");
        earn(pos);
        earn(pos);
        assertThat(account()).containsEntry("points_balance", 1234);
        assertThat((BigDecimal) account().get("total_spent")).isEqualByComparingTo("1234567");
        assertThat(jdbc.queryForObject("select count(*) from loyalty_transactions where reference_id = ?", Integer.class, pos)).isEqualTo(1);
        assertThat(currentTier()).isEqualTo("ZQXW_SILVER");
    }

    @Test
    void walkInParentAndUnfinishedOrdersEarnNothing() {
        earn(order("OFFLINE", "COMPLETED", null, store, null, "500000", "0"));
        earn(order("ONLINE", "SHIPPING", customer, store, null, "500000", "0"));
        earn(order("ONLINE", "COMPLETED", customer, null, null, "500000", "0"));
        assertThat(jdbc.queryForObject("select count(*) from loyalty_accounts where customer_id = ?", Integer.class, customer)).isZero();
    }

    @Test
    void splitPartEarnsOnItsShareAfterOrderVoucher() {
        UUID parent = order("ONLINE", "SHIPPING", customer, null, null, "900000", "100000");
        UUID first = order("ONLINE", "COMPLETED", customer, store, parent, "600000", "0");
        order("ONLINE", "SHIPPING", customer, store, parent, "400000", "0");
        earn(first);
        assertThat(account()).containsEntry("points_balance", 540);
        assertThat((BigDecimal) account().get("total_spent")).isEqualByComparingTo("540000");
    }

    @Test
    void refundReversesUpToBalanceAndNeverBelowZero() {
        UUID pos = order("OFFLINE", "COMPLETED", customer, store, null, "1000000", "0");
        earn(pos);
        jdbc.update("update loyalty_accounts set points_balance = 300 where customer_id = ?", customer);
        Payment payment = Payment.builder().orderId(pos).amount(new BigDecimal("1000000")).build();

        loyalty.reverseForRefund(refund(pos, "500500"), payment);
        assertThat(account()).containsEntry("points_balance", 0);
        assertThat((BigDecimal) account().get("total_spent")).isEqualByComparingTo("499500");
        assertThat(jdbc.queryForObject("select note from loyalty_transactions where transaction_type = 'REFUND_REVERSAL' "
                + "and reference_id = ?", String.class, pos)).contains("không trừ được 200 điểm");

        loyalty.reverseForRefund(refund(pos, "900000"), payment);
        assertThat((BigDecimal) account().get("total_spent")).isEqualByComparingTo("0");
        assertThat(currentTier()).isEqualTo("ZQXW_SILVER");
    }

    @Test
    void refundOfOrderWithoutPointsChangesNothing() {
        UUID cancelled = order("ONLINE", "CANCELLED", customer, store, null, "800000", "0");
        loyalty.reverseForRefund(refund(cancelled, "800000"), Payment.builder().orderId(cancelled).build());
        assertThat(jdbc.queryForObject("select count(*) from loyalty_transactions where customer_id = ?", Integer.class, customer)).isZero();
    }

    @Test
    void deliveryAndRefundCompletionTriggerTheLedger() {
        UUID online = order("ONLINE", "SHIPPING", customer, store, null, "250000", "0");
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_LOYALTY")) {
            statusService.transition(orders.findByIdForUpdate(online).orElseThrow(), OrderStatus.COMPLETED,
                    OrderActor.shipper(), "Giao thành công");
            assertThat(account()).containsEntry("points_balance", 250);

            UUID paymentId = UUID.randomUUID();
            jdbc.update("insert into payments(id, order_id, method, amount, status) values (?, ?, 'VNPAY', 250000, 'SUCCESS')", paymentId, online);
            UUID refundId = UUID.randomUUID();
            jdbc.update("insert into refunds(id, payment_id, order_id, refund_code, amount, status) values (?, ?, ?, 'ZQXW-RF', 100000, 'PENDING')",
                    refundId, paymentId, online);
            refundService.complete(refundId, null, null);
        }
        assertThat(account()).containsEntry("points_balance", 150);
    }

    private static Refund refund(UUID orderId, String amount) {
        return Refund.builder().orderId(orderId).refundCode("ZQXW-RF").amount(new BigDecimal(amount)).build();
    }
}
