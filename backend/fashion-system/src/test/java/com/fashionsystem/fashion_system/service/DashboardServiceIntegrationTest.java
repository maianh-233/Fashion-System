package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse;
import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse.RevenuePoint;
import com.fashionsystem.fashion_system.repository.DashboardQueryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Số liệu trang tổng quan trên cơ sở dữ liệu thật; so sánh chênh lệch trước/sau khi thêm dữ liệu mẫu để không phụ
 * thuộc dữ liệu sẵn có. Phạm vi nhân viên được giả lập; dữ liệu rollback sau mỗi test.
 */
@SpringBootTest
@Transactional
class DashboardServiceIntegrationTest {
    @Autowired DashboardService dashboard;
    @Autowired DashboardQueryRepository queries;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean StoreAccessService access;
    final UUID actor = UUID.randomUUID();
    UUID store;
    UUID otherStore;
    Map<String, Object> variant;

    @BeforeEach
    void setUp() {
        List<UUID> stores = jdbc.queryForList("select id from stores where active = true order by code limit 2", UUID.class);
        store = stores.get(0);
        otherStore = stores.get(1);
        variant = jdbc.queryForMap("select v.id, v.product_id from product_variants v join products p on p.id = v.product_id "
                + "where v.active = true order by v.sku limit 1");
        when(access.requireOrAll(eq(actor), any(), anyString())).thenAnswer(call -> call.getArgument(1));
        when(access.allowsScope(eq(actor), any(), anyString())).thenReturn(true);
    }

    private UUID order(String type, String status, UUID storeId, UUID parent, String total, String paymentStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into orders(id, order_code, order_type, status, active, store_id, parent_order_id, subtotal, "
                        + "discount_total, shipping_fee, total_amount, payment_status, created_at) "
                        + "values (?, ?, ?, ?, true, ?, ?, ?, 0, 0, ?, ?, now() at time zone 'UTC')",
                id, "ZQDB-" + id.toString().substring(0, 12), type, status, storeId, parent,
                new BigDecimal(total), new BigDecimal(total), paymentStatus);
        return id;
    }

    private void item(UUID orderId, int quantity, String total) {
        jdbc.update("insert into order_items(order_id, product_id, product_variant_id, product_name, price, quantity, total) "
                        + "values (?, ?, ?, 'Zqdb Áo thử', ?, ?, ?)",
                orderId, variant.get("product_id"), variant.get("id"), new BigDecimal(total), quantity, new BigDecimal(total));
    }

    private static BigDecimal today(List<RevenuePoint> points) {
        return points.get(points.size() - 1).revenue();
    }

    @Test
    void storeScopeCountsCompletedStoreOrdersOnly() {
        DashboardResponse before = dashboard.overview(actor, store);

        UUID pos = order("OFFLINE", "COMPLETED", store, null, "1000000", "PAID");
        item(pos, 2, "1000000");
        order("ONLINE", "CANCELLED", store, null, "500000", "UNPAID");
        UUID awaiting = order("ONLINE", "PENDING", store, null, "300000", "AWAITING_PAYMENT");
        order("ONLINE", "CONFIRMED", store, null, "200000", "UNPAID");
        order("OFFLINE", "COMPLETED", otherStore, null, "900000", "PAID");
        jdbc.update("insert into customers(id, customer_code, membership_status, source, origin_store_id, created_at) "
                + "values (?, ?, 'MEMBER', 'STORE', ?, now() at time zone 'UTC')", UUID.randomUUID(), "ZQDB-CUS", store);

        DashboardResponse after = dashboard.overview(actor, store);
        assertThat(after.month().revenue().subtract(before.month().revenue())).isEqualByComparingTo("1000000");
        assertThat(after.month().orders() - before.month().orders()).isEqualTo(2);
        assertThat(after.month().itemsSold() - before.month().itemsSold()).isEqualTo(2);
        assertThat(after.month().newCustomers() - before.month().newCustomers()).isEqualTo(1);
        assertThat(today(after.dailyRevenue()).subtract(today(before.dailyRevenue()))).isEqualByComparingTo("1000000");
        assertThat(today(after.yearlyRevenue()).subtract(today(before.yearlyRevenue()))).isEqualByComparingTo("1000000");
        assertThat(after.dailyRevenue()).hasSize(7);
        assertThat(after.yearlyRevenue()).hasSize(5);
        assertThat(after.recentOrders()).extracting(DashboardResponse.RecentOrder::orderCode)
                .contains("ZQDB-" + pos.toString().substring(0, 12))
                .doesNotContain("ZQDB-" + awaiting.toString().substring(0, 12));
        assertThat(after.recentOrders()).allMatch(row -> row.storeName() != null);
        assertThat(after.bestSellers()).anyMatch(row -> row.productName().equals("Zqdb Áo thử") && row.quantity() == 2);
    }

    @Test
    void wholeChainSkipsSplitParentsSoRevenueIsNotCountedTwice() {
        DashboardResponse before = dashboard.overview(actor, null);

        UUID parent = order("ONLINE", "COMPLETED", null, null, "700000", "PAID");
        order("ONLINE", "COMPLETED", store, parent, "400000", "PAID");
        order("ONLINE", "COMPLETED", otherStore, parent, "300000", "PAID");

        DashboardResponse after = dashboard.overview(actor, null);
        assertThat(after.storeId()).isNull();
        assertThat(after.month().revenue().subtract(before.month().revenue())).isEqualByComparingTo("700000");
        assertThat(after.month().orders() - before.month().orders()).isEqualTo(2);
    }

    @Test
    void listsTheEmployeeMayNotSeeAreLeftOut() {
        when(access.allowsScope(eq(actor), any(), anyString())).thenReturn(false);
        DashboardResponse response = dashboard.overview(actor, store);
        assertThat(response.recentOrders()).isNull();
        assertThat(response.lowStock()).isNull();
        assertThat(response.recentImports()).isNull();
        assertThat(response.recentExports()).isNull();
        assertThat(response.month()).isNotNull();
    }

    @Test
    void lowStockUsesWarehouseThresholdAndIgnoresSoldOut() {
        List<UUID> variants = jdbc.queryForList("select b.product_variant_id from inventory_balances b "
                + "join product_variants v on v.id = b.product_variant_id where b.store_id = ? and v.active = true "
                + "order by v.sku limit 2", UUID.class, store);
        jdbc.update("update inventory_balances set available_quantity = 2, online_quantity = 1 "
                + "where store_id = ? and product_variant_id = ?", store, variants.get(0));
        jdbc.update("update inventory_balances set available_quantity = 0, online_quantity = 0 "
                + "where store_id = ? and product_variant_id = ?", store, variants.get(1));

        List<DashboardResponse.LowStockItem> rows = queries.lowStock(false, store, DashboardService.LOW_STOCK_THRESHOLD,
                PageRequest.of(0, 10_000));
        assertThat(rows).anyMatch(row -> row.productVariantId().equals(variants.get(0)) && row.quantity() == 3);
        assertThat(rows).noneMatch(row -> row.productVariantId().equals(variants.get(1)));
        assertThat(rows).allMatch(row -> row.storeId().equals(store) && row.quantity() >= 1 && row.quantity() <= 5);
    }
}
