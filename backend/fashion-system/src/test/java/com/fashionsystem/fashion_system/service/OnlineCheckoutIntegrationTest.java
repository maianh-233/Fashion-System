package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.order.OfflineCartRequest;
import com.fashionsystem.fashion_system.dto.order.OnlineCheckoutRequest;
import com.fashionsystem.fashion_system.dto.order.OnlineOrderPlacedResponse;
import com.fashionsystem.fashion_system.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Luồng đặt đơn online COD trên cơ sở dữ liệu thật: giỏ hàng → phân cửa hàng → trừ tồn online → tạo đơn.
 * Mọi dữ liệu do test tự tạo (cửa hàng, sản phẩm, khách) và rollback sau mỗi test.
 */
@SpringBootTest
@Transactional
class OnlineCheckoutIntegrationTest {
    @Autowired OnlineOrderService onlineOrders;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    final UUID store = UUID.randomUUID();
    final UUID variant = UUID.randomUUID();
    final UUID customer = UUID.randomUUID();
    final UUID address = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        String tag = store.toString().substring(0, 8);
        // Tọa độ lẻ để không trùng ràng buộc tọa độ cửa hàng duy nhất trên database có sẵn dữ liệu.
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, 'Zqoc Store', true, 10.7412345, 106.6612345)",
                store, "ZQOC-" + tag);
        UUID product = UUID.randomUUID();
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, ?, 'Zqoc Đầm lụa', ?, 'ACTIVE', '')",
                product, "ZQOC-" + tag, "zqoc-dam-" + tag);
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Đỏ', 'M', 250000, true)",
                variant, product, "ZQOC-" + tag + "-M");
        jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, 0, 5, 0)",
                store, variant);

        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name) values (?, ?, 'MEMBER', 'WEBSITE', 'Zqoc Khách')",
                customer, "ZQOC-" + tag);
        jdbc.update("insert into customer_addresses(id, customer_id, receiver_name, receiver_phone, address_line, latitude, longitude, "
                + "is_default, address_type, management_source) values (?, ?, 'Zqoc Khách', '0900000000', '1 Lê Lợi', 10.7420000, 106.6620000, "
                + "true, 'HOME', 'WEB')", address, customer);
        jdbc.update("insert into cart_items(customer_id, product_variant_id, quantity) values (?, ?, 2)", customer, variant);
    }

    @Test
    void codOrderDeductsOnlineStockAndEmptiesTheCart() {
        OnlineOrderPlacedResponse placed = place(request(2, "req-1"));
        sync();

        assertThat(placed.split()).isFalse();
        assertThat(placed.paymentMethod()).isEqualTo("COD");
        assertThat(placed.total()).isEqualByComparingTo(new BigDecimal("500000"));

        Map<String, Object> order = jdbc.queryForMap(
                "select store_id, customer_id, order_type, status, payment_status from orders where id = ?", placed.id());
        assertThat(order).containsEntry("store_id", store)
                .containsEntry("customer_id", customer)
                .containsEntry("order_type", "ONLINE")
                .containsEntry("status", "PENDING")
                .containsEntry("payment_status", "UNPAID");
        assertThat(jdbc.queryForObject("select sum(quantity) from order_items where order_id = ?", Integer.class, placed.id()))
                .isEqualTo(2);
        assertThat(onlineStock()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from cart_items where customer_id = ?", Integer.class, customer)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from order_addresses where order_id = ?", Integer.class, placed.id()))
                .isEqualTo(1);
    }

    @Test
    void retryWithSameClientRequestIdReturnsTheSameOrderWithoutDeductingAgain() {
        OnlineOrderPlacedResponse first = place(request(2, "req-dup"));
        sync();
        OnlineOrderPlacedResponse retry = place(request(2, "req-dup"));
        sync();

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(onlineStock()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id = ?", Integer.class, customer)).isEqualTo(1);
    }

    @Test
    void orderingMoreThanOnlineStockIsRejectedAndChangesNothing() {
        assertThatThrownBy(() -> place(request(6, "req-big")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chỉ còn 5");
        sync();

        assertThat(onlineStock()).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from orders where customer_id = ?", Integer.class, customer)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from cart_items where customer_id = ?", Integer.class, customer)).isEqualTo(1);
    }

    /** Như CustomerOrderController: khách tự đặt đơn chạy trong system job của business audit. */
    private OnlineOrderPlacedResponse place(OnlineCheckoutRequest request) {
        try (var ignored = BusinessAuditContextFactory.systemJob("CUSTOMER_ORDER:" + customer)) {
            return onlineOrders.place(customer, request);
        }
    }

    private OnlineCheckoutRequest request(int quantity, String clientRequestId) {
        return new OnlineCheckoutRequest(address, List.of(new OfflineCartRequest.Line(variant, quantity)),
                List.of(), "COD", null, clientRequestId);
    }

    private int onlineStock() {
        return jdbc.queryForObject("select online_quantity from inventory_balances where store_id = ? and product_variant_id = ?",
                Integer.class, store, variant);
    }

    /** Ghi xuống DB và bỏ cache JPA để đọc bằng JDBC thấy cùng dữ liệu. */
    private void sync() {
        entityManager.flush();
        entityManager.clear();
    }
}
