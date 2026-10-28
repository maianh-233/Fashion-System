package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.order.ReturnDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/** Đổi / trả hàng trên cơ sở dữ liệu thật; phạm vi cửa hàng được giả lập, dữ liệu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class ReturnRequestIntegrationTest {
    @Autowired ReturnRequestService returns;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @MockitoBean StoreAccessService storeAccess;

    final UUID store = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID bought = UUID.randomUUID();
    final UUID other = UUID.randomUUID();
    final UUID customer = UUID.randomUUID();
    final UUID actor = UUID.randomUUID();
    UUID order;
    UUID line;

    @BeforeEach
    void setUp() {
        String tag = product.toString().substring(0, 8);
        jdbc.update("insert into users(id, username) values (?, ?)", actor, "zqrt-" + tag);
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, 'Zqrt Store', true)", store, "ZQRT-" + tag);
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, ?, 'Zqrt Quần jean', ?, 'ACTIVE', '')",
                product, "ZQRT-" + tag, "zqrt-quan-" + tag);
        variant(bought, "ZQRT-" + tag + "-M", "M", 0);
        variant(other, "ZQRT-" + tag + "-L", "L", 2);
        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name) values (?, ?, 'MEMBER', 'WEBSITE', 'Zqrt Khách')",
                customer, "ZQRT-" + tag);
        order = completedOrder("CASH", 0);
        when(storeAccess.require(eq(actor), eq(store), anyString())).thenReturn(store);
        when(storeAccess.requireOrAll(eq(actor), any(), anyString())).thenReturn(store);
    }

    @Test
    void returnRefundsAtTheCounterAndPutsGoodItemsBackOnTheShelf() {
        ReturnDtos.Eligibility eligibility = returns.eligibility(customer, order);
        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.items()).singleElement().satisfies(item -> {
            assertThat(item.requestable()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("270000");
            assertThat(item.exchangeOptions()).singleElement().satisfies(o -> {
                assertThat(o.size()).isEqualTo("L");
                assertThat(o.inStock()).isTrue();
            });
        });

        ReturnDtos.ReturnRequestView created = asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "RETURN", "WRONG_SIZE", " Rộng quá ", List.of(new ReturnDtos.CreateItem(line, 1, null)))));
        assertThat(created.status()).isEqualTo("REQUESTED");
        assertThat(created.requestCode()).startsWith("RT-");
        assertThat(created.note()).isEqualTo("Rộng quá");
        assertThat(created.estimatedRefund()).isEqualByComparingTo("270000");
        assertThat(returns.eligibility(customer, order).items().getFirst().requestable()).isEqualTo(1);
        assertThatThrownBy(() -> asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "RETURN", "OTHER", null, List.of(new ReturnDtos.CreateItem(line, 2, null))))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("chỉ còn 1");

        asJob(() -> returns.approve(actor, created.id()));
        ReturnDtos.ReturnRequestView done = asJob(() -> returns.complete(actor, created.id(),
                new ReturnDtos.CompleteRequest(Map.of(created.items().getFirst().id(), "GOOD"))));

        assertThat(done.status()).isEqualTo("COMPLETED");
        assertThat(done.refundAmount()).isEqualByComparingTo("270000");
        assertThat(done.refundStatus()).isEqualTo("COMPLETED");
        assertThat(stock(bought, "available_quantity")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where reference_id = ? and transaction_type = 'CUSTOMER_RETURN'",
                Integer.class, created.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select order_id from refunds where refund_code = ?", UUID.class, created.requestCode())).isEqualTo(order);
    }

    @Test
    void exchangeIssuesTheNewSizeAndSendsDefectsToDamagedStock() {
        ReturnDtos.ReturnRequestView created = asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "EXCHANGE", "DEFECTIVE", null, List.of(new ReturnDtos.CreateItem(line, 2, other)))));
        assertThat(created.items().getFirst().exchangeSize()).isEqualTo("L");
        asJob(() -> returns.approve(actor, created.id()));

        ReturnDtos.ReturnRequestView done = asJob(() -> returns.complete(actor, created.id(),
                new ReturnDtos.CompleteRequest(Map.of(created.items().getFirst().id(), "DAMAGED"))));

        assertThat(done.status()).isEqualTo("COMPLETED");
        assertThat(done.refundAmount()).isNull();
        assertThat(stock(bought, "damaged_quantity")).isEqualTo(2);
        assertThat(stock(bought, "available_quantity")).isZero();
        assertThat(stock(other, "available_quantity")).isZero();
    }

    @Test
    void exchangeNeedsCounterStockAndASiblingVariant() {
        assertThatThrownBy(() -> asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "EXCHANGE", "WRONG_SIZE", null, List.of(new ReturnDtos.CreateItem(line, 1, null))))))
                .hasMessageContaining("chọn phân loại");
        assertThatThrownBy(() -> asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "EXCHANGE", "WRONG_SIZE", null, List.of(new ReturnDtos.CreateItem(line, 1, bought))))))
                .hasMessageContaining("khác phân loại");

        jdbc.update("update inventory_balances set available_quantity = 0 where product_variant_id = ?", other);
        ReturnDtos.ReturnRequestView created = asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "EXCHANGE", "WRONG_SIZE", null, List.of(new ReturnDtos.CreateItem(line, 1, other)))));
        asJob(() -> returns.approve(actor, created.id()));
        assertThatThrownBy(() -> asJob(() -> returns.complete(actor, created.id(),
                new ReturnDtos.CompleteRequest(Map.of(created.items().getFirst().id(), "GOOD")))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không đủ để đổi");
    }

    @Test
    void gatewayRefundWaitsForConfirmation() {
        UUID paidOnline = completedOrder("VNPAY", 0);
        UUID onlineLine = jdbc.queryForObject("select id from order_items where order_id = ?", UUID.class, paidOnline);
        ReturnDtos.ReturnRequestView created = asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(paidOnline, "RETURN", "CHANGED_MIND", null, List.of(new ReturnDtos.CreateItem(onlineLine, 2, null)))));
        asJob(() -> returns.approve(actor, created.id()));
        ReturnDtos.ReturnRequestView done = asJob(() -> returns.complete(actor, created.id(),
                new ReturnDtos.CompleteRequest(Map.of(created.items().getFirst().id(), "GOOD"))));
        assertThat(done.refundStatus()).isEqualTo("PENDING");

        ReturnDtos.ReturnRequestView confirmed = asJob(() -> returns.confirmRefund(actor, created.id(), "VNP-123"));
        assertThat(confirmed.refundStatus()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("select status from payments where order_id = ?", String.class, paidOnline)).isEqualTo("REFUNDED");
    }

    @Test
    void windowStatusAndOwnershipRules() {
        UUID old = completedOrder("CASH", 20);
        assertThat(returns.eligibility(customer, old).eligible()).isFalse();
        assertThat(returns.eligibility(customer, old).reason()).contains("14 ngày");

        jdbc.update("update orders set status = 'CONFIRMED' where id = ?", order);
        entityManager.clear();
        assertThat(returns.eligibility(customer, order).reason()).contains("đã hoàn thành");
        jdbc.update("update orders set status = 'COMPLETED' where id = ?", order);
        entityManager.clear();

        assertThatThrownBy(() -> returns.eligibility(UUID.randomUUID(), order)).hasMessageContaining("không tồn tại");

        ReturnDtos.ReturnRequestView created = asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "RETURN", "OTHER", null, List.of(new ReturnDtos.CreateItem(line, 1, null)))));
        assertThat(asJob(() -> returns.cancel(customer, created.id())).status()).isEqualTo("CANCELLED");
        assertThat(returns.eligibility(customer, order).items().getFirst().requestable()).isEqualTo(2);
        assertThatThrownBy(() -> asJob(() -> returns.approve(actor, created.id()))).hasMessageContaining("chờ duyệt");

        ReturnDtos.ReturnRequestView again = asJob(() -> returns.create(customer,
                new ReturnDtos.CreateRequest(order, "RETURN", "OTHER", null, List.of(new ReturnDtos.CreateItem(line, 1, null)))));
        ReturnDtos.ReturnRequestView rejected = asJob(() -> returns.reject(actor, again.id(), "Sản phẩm đã qua sử dụng"));
        assertThat(rejected.rejectReason()).isEqualTo("Sản phẩm đã qua sử dụng");
        assertThat(returns.mine(customer, order)).extracting(ReturnDtos.ReturnRequestView::status).containsExactly("REJECTED", "CANCELLED");
        assertThat(returns.list(actor, null, "REJECTED", "Zqrt", Pageable.ofSize(10)).getContent())
                .extracting(ReturnDtos.ReturnRequestView::id).containsExactly(again.id());
    }

    private <T> T asJob(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_RETURN")) {
            return action.get();
        }
    }

    private void variant(UUID id, String sku, String size, int counterStock) {
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Xanh', ?, 300000, true)",
                id, product, sku, size);
        jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, ?, 0, 0)",
                store, id, counterStock);
    }

    /** Đơn 2 món (đã giảm 30.000đ / món) hoàn thành {@code daysAgo} ngày trước, thanh toán {@code method} thành công. */
    private UUID completedOrder(String method, int daysAgo) {
        UUID id = UUID.randomUUID();
        String type = "CASH".equals(method) ? "OFFLINE" : "ONLINE";
        jdbc.update("insert into orders(id, order_code, order_type, status, active, store_id, customer_id, subtotal, discount_total, shipping_fee, "
                        + "total_amount, payment_status, payment_method, created_at) values (?, ?, ?, 'COMPLETED', true, ?, ?, 600000, 60000, 0, "
                        + "540000, 'PAID', ?, now() at time zone 'UTC' - make_interval(days => ?))",
                id, "ZQRT-" + id.toString().substring(0, 12), type, store, customer, method, daysAgo + 1);
        UUID item = UUID.randomUUID();
        jdbc.update("insert into order_items(id, order_id, product_id, product_variant_id, product_name, color, size, price, quantity, discount_amount, total) "
                + "values (?, ?, ?, ?, 'Zqrt Quần jean', 'Xanh', 'M', 300000, 2, 60000, 540000)", item, id, product, bought);
        jdbc.update("insert into order_status_histories(order_id, from_status, to_status, actor_type, changed_at) "
                + "values (?, 'CONFIRMED', 'COMPLETED', 'USER', now() at time zone 'UTC' - make_interval(days => ?))", id, daysAgo);
        jdbc.update("insert into payments(order_id, payment_code, method, amount, status, paid_at, created_at) values (?, ?, ?, 540000, 'SUCCESS', "
                + "now() at time zone 'UTC', now() at time zone 'UTC')", id, "ZQRT-" + id.toString().substring(0, 12), method);
        if (line == null) line = item;
        return id;
    }

    private int stock(UUID variantId, String column) {
        return jdbc.queryForObject("select " + column + " from inventory_balances where store_id = ? and product_variant_id = ?",
                Integer.class, store, variantId);
    }
}
