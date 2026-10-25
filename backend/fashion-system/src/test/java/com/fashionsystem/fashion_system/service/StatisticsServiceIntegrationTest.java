package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.Breakdown;
import com.fashionsystem.fashion_system.dto.statistics.StatisticsResponse.SeriesPoint;
import com.fashionsystem.fashion_system.entity.OrderType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo thống kê trên cơ sở dữ liệu thật; so sánh chênh lệch trước/sau khi thêm dữ liệu mẫu nên không phụ thuộc
 * dữ liệu sẵn có. Phạm vi nhân viên được giả lập; dữ liệu rollback sau mỗi test.
 */
@SpringBootTest
@Transactional
class StatisticsServiceIntegrationTest {
    @Autowired StatisticsService statistics;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean StoreAccessService access;
    final UUID actor = UUID.randomUUID();
    final UUID customer = UUID.randomUUID();
    UUID store;
    Map<String, Object> variant;

    @BeforeEach
    void setUp() {
        store = jdbc.queryForObject("select id from stores where active = true order by code limit 1", UUID.class);
        // Biến thể chưa từng nhập kho hoàn thành: giá nhập bình quân chỉ đến từ phiếu nhập của test.
        variant = jdbc.queryForMap("""
                select v.id, v.product_id from product_variants v
                where v.active = true and not exists (
                    select 1 from goods_receipt_items ri join goods_receipts g on g.id = ri.receipt_id
                    where ri.product_variant_id = v.id and g.status = 'COMPLETED')
                order by v.sku limit 1
                """);
        when(access.requireOrAll(eq(actor), any(), anyString())).thenAnswer(call -> call.getArgument(1));
        jdbc.update("insert into customers(id, customer_code, membership_status, source, origin_store_id, full_name, created_at) "
                + "values (?, ?, 'MEMBER', 'STORE', ?, 'Zqst Khách VIP', now() at time zone 'UTC')",
                customer, "ZQST-" + customer.toString().substring(0, 8), store);
        UUID supplier = jdbc.queryForObject("select id from suppliers order by code limit 1", UUID.class);
        UUID receipt = UUID.randomUUID();
        jdbc.update("insert into goods_receipts(id, receipt_code, store_id, supplier_id, status, total_quantity, total_amount, "
                + "completed_at) values (?, ?, ?, ?, 'COMPLETED', 10, 3000000, now() at time zone 'UTC')",
                receipt, "ZQST-" + receipt, store, supplier);
        jdbc.update("insert into goods_receipt_items(receipt_id, product_id, product_variant_id, cost_price, quantity, total, "
                + "target_channel) values (?, ?, ?, 300000, 10, 3000000, 'OFFLINE')",
                receipt, variant.get("product_id"), variant.get("id"));
    }

    /** Giờ địa phương của ứng dụng đổi sang UTC như Hibernate lưu xuống DB. */
    private static LocalDateTime utc(LocalDateTime local) {
        return local.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private UUID order(String type, String status, UUID customerId, String total, String shipping, int quantity,
                       LocalDateTime createdLocal) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into orders(id, order_code, order_type, status, active, store_id, customer_id, subtotal, "
                        + "discount_total, shipping_fee, total_amount, payment_status, payment_method, created_at) "
                        + "values (?, ?, ?, ?, true, ?, ?, ?, 0, ?, ?, 'PAID', ?, ?)",
                id, "ZQST-" + id.toString().substring(0, 12), type, status, store, customerId,
                new BigDecimal(total), new BigDecimal(shipping), new BigDecimal(total),
                type.equals("ONLINE") ? "VNPAY" : "CASH", utc(createdLocal));
        BigDecimal goods = new BigDecimal(total).subtract(new BigDecimal(shipping));
        jdbc.update("insert into order_items(order_id, product_id, product_variant_id, product_name, price, quantity, total) "
                        + "values (?, ?, ?, 'Zqst Váy thử', ?, ?, ?)",
                id, variant.get("product_id"), variant.get("id"), goods, quantity, goods);
        return id;
    }

    private StatisticsResponse report(OrderType type) {
        return statistics.report(actor, store, StatisticsPeriod.LAST_7_DAYS, type);
    }

    private static SeriesPoint last(List<SeriesPoint> series) {
        return series.get(series.size() - 1);
    }

    private static long count(Map<String, Long> counts, String key) {
        return counts.getOrDefault(key, 0L);
    }

    private static Breakdown key(List<Breakdown> rows, String key) {
        return rows.stream().filter(row -> row.key().equals(key)).findFirst().orElse(new Breakdown(key, 0, BigDecimal.ZERO));
    }

    @Test
    void reportAggregatesRevenueProfitAndBreakdowns() {
        StatisticsResponse before = report(null);
        LocalDateTime justAfterMidnight = LocalDate.now().atTime(0, 30);
        order("OFFLINE", "COMPLETED", customer, "1000000", "0", 2, justAfterMidnight);
        order("ONLINE", "COMPLETED", customer, "530000", "30000", 1, justAfterMidnight);
        order("ONLINE", "CANCELLED", null, "400000", "0", 1, justAfterMidnight);
        StatisticsResponse after = report(null);

        assertThat(after.overview().revenue().subtract(before.overview().revenue())).isEqualByComparingTo("1530000");
        assertThat(after.overview().orders() - before.overview().orders()).isEqualTo(2);
        // (1.000.000 − 2×300.000) + (530.000 − 30.000 phí ship − 300.000)
        assertThat(after.overview().grossProfit().subtract(before.overview().grossProfit())).isEqualByComparingTo("600000");
        assertThat(after.overview().newCustomers()).isPositive();
        // 00:30 giờ địa phương là ngày hôm trước theo UTC nhưng vẫn phải nằm ở mốc hôm nay.
        assertThat(last(after.revenueSeries()).label()).isEqualTo(LocalDate.now().toString());
        assertThat(last(after.revenueSeries()).revenue().subtract(last(before.revenueSeries()).revenue()))
                .isEqualByComparingTo("1530000");
        assertThat(after.revenueSeries()).hasSize(7);
        assertThat(count(after.statusCounts(), "CANCELLED") - count(before.statusCounts(), "CANCELLED")).isEqualTo(1);
        assertThat(key(after.byOrderType(), "ONLINE").orders() - key(before.byOrderType(), "ONLINE").orders()).isEqualTo(1);
        assertThat(key(after.byPaymentMethod(), "CASH").revenue().subtract(key(before.byPaymentMethod(), "CASH").revenue()))
                .isEqualByComparingTo("1000000");
        assertThat(after.topProducts()).anyMatch(row -> row.productName().equals("Zqst Váy thử") && row.sold() >= 3);
        assertThat(after.vipCustomers()).anyMatch(row -> row.id().equals(customer) && row.orders() == 2
                && row.spend().compareTo(new BigDecimal("1530000")) == 0);
        assertThat(after.retention().returningCustomers() - before.retention().returningCustomers()).isEqualTo(1);
        assertThat(after.newCustomersByMonth()).hasSize(6);
        assertThat(after.newCustomersByMonth().get(5).month()).isEqualTo(YearMonth.now().toString());
        assertThat(after.newCustomersByMonth().get(5).count()).isPositive();
        assertThat(after.inventory().importedQuantity()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void orderTypeFilterNarrowsOrderMetricsOnly() {
        StatisticsResponse before = report(OrderType.ONLINE);
        LocalDateTime now = LocalDateTime.now().minusMinutes(1);
        order("OFFLINE", "COMPLETED", customer, "1000000", "0", 2, now);
        order("ONLINE", "COMPLETED", customer, "530000", "30000", 1, now);
        StatisticsResponse after = report(OrderType.ONLINE);

        assertThat(after.orderType()).isEqualTo(OrderType.ONLINE);
        assertThat(after.overview().revenue().subtract(before.overview().revenue())).isEqualByComparingTo("530000");
        assertThat(after.overview().grossProfit().subtract(before.overview().grossProfit())).isEqualByComparingTo("200000");
        assertThat(key(after.byOrderType(), "OFFLINE").orders()).isZero();
        // Khách mới không phụ thuộc loại đơn.
        assertThat(after.overview().newCustomers()).isEqualTo(report(null).overview().newCustomers()).isPositive();
    }

    @Test
    void ordersOutsideThePeriodCountForThePreviousPeriod() {
        StatisticsResponse before = report(null);
        order("OFFLINE", "COMPLETED", customer, "800000", "0", 1, LocalDate.now().minusDays(10).atTime(12, 0));
        StatisticsResponse after = report(null);

        assertThat(after.overview().revenue()).isEqualByComparingTo(before.overview().revenue());
        assertThat(after.overview().previousRevenue().subtract(before.overview().previousRevenue()))
                .isEqualByComparingTo("800000");
    }
}
