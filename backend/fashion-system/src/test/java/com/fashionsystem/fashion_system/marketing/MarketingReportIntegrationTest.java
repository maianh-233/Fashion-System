package com.fashionsystem.fashion_system.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.marketing.MarketingReportDtos.Kpi;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo chỉ đọc nên chạy trong một giao dịch rollback. Dữ liệu đơn hàng được dựng bằng SQL để kiểm soát chính xác
 * từng trường hợp đếm trùng / loại trừ.
 */
@SpringBootTest
@Transactional
class MarketingReportIntegrationTest {
    @Autowired MarketingReportService reports;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean MarketingCampaignNotifier notifier;

    final String tag = UUID.randomUUID().toString().substring(0, 8);
    final UUID admin = UUID.randomUUID();
    final UUID storeStaff = UUID.randomUUID();
    final UUID store = UUID.randomUUID();
    final UUID campaignA = UUID.randomUUID();
    final UUID campaignB = UUID.randomUUID();
    final UUID promoA = UUID.randomUUID();
    final UUID promoB = UUID.randomUUID();
    final UUID flash = UUID.randomUUID();
    final UUID customerX = UUID.randomUUID();
    final UUID customerY = UUID.randomUUID();
    final UUID o1 = UUID.randomUUID();
    final UUID o2 = UUID.randomUUID();
    final UUID o2a = UUID.randomUUID();
    final UUID o2b = UUID.randomUUID();
    final UUID o3 = UUID.randomUUID();
    final UUID o4 = UUID.randomUUID();
    final UUID o5 = UUID.randomUUID();
    int codes;

    @BeforeEach
    void setUp() {
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, ?, true, ?, ?)",
                store, "ZMR" + tag, "Zmr Store", 10 + Math.random(), 100 + Math.random());
        user(admin, "admin", "ADMIN", null);
        user(storeStaff, "staff", "STAFF", store);
        customer(customerX, "x");
        customer(customerY, "y");
        campaign(campaignA, "A", 60_000);
        campaign(campaignB, "B", 0);
        promotion(promoA);
        promotion(promoB);
        Map<String, Object> variant = jdbc.queryForMap("select id, product_id from product_variants limit 1");
        UUID variantId = (UUID) variant.get("id");
        jdbc.update("""
                insert into flash_sales(id, name, starts_at, ends_at, active)
                values (?, ?, (now() at time zone 'UTC') - interval '5 day', (now() at time zone 'UTC') + interval '5 day', true)""",
                flash, "Zmr flash " + tag);
        UUID flashItem = UUID.randomUUID();
        jdbc.update("insert into flash_sale_items(id, flash_sale_id, product_variant_id, sale_price, quota) values (?, ?, ?, 80, 100)",
                flashItem, flash, variantId);
        link(campaignA, "PROMOTION", "promotion_id", promoA);
        link(campaignA, "FLASH_SALE", "flash_sale_id", flash);
        link(campaignB, "PROMOTION", "promotion_id", promoB);
        jdbc.update("insert into marketing_campaign_expenses(campaign_id, category, amount, spent_on) values (?, 'ADS', 60, current_date)", campaignA);

        // o1: tại quầy, hoàn tất, dùng khuyến mãi của A.
        order(o1, null, "OFFLINE", "COMPLETED", 500, customerX, "1 day");
        promoUse(o1, promoA, 50);
        // o2: đơn online tách 2 cửa hàng; ưu đãi ghi trên đơn gốc, dùng cả khuyến mãi của A và B.
        order(o2, null, "ONLINE", "COMPLETED", 300, customerY, "1 day");
        order(o2a, o2, "ONLINE", "COMPLETED", 150, customerY, "1 day");
        order(o2b, o2, "ONLINE", "COMPLETED", 150, customerY, "1 day");
        promoUse(o2, promoA, 30);
        promoUse(o2, promoB, 20);
        // o3: đang xử lý, mua 2 món giá flash (giá thường 100, giá flash 80).
        order(o3, null, "ONLINE", "PENDING", 200, customerX, "1 day");
        jdbc.update("insert into order_items(order_id, product_id, product_variant_id, product_name, price, quantity, total) values (?, ?, ?, 'Zmr', 100, 2, 160)",
                o3, variant.get("product_id"), variantId);
        jdbc.update("insert into flash_sale_usages(flash_sale_item_id, order_id, customer_id, quantity) values (?, ?, ?, 2)", flashItem, o3, customerX);
        // o4: đã hủy; o5: trước khi chiến dịch bắt đầu — đều không được tính.
        order(o4, null, "OFFLINE", "CANCELLED", 999, customerX, "1 day");
        promoUse(o4, promoA, 99);
        order(o5, null, "OFFLINE", "COMPLETED", 777, customerX, "20 day");
        promoUse(o5, promoA, 77);
        // Trả hàng hoàn tất trên ĐƠN CON của o2: phải quy về o2.
        jdbc.update("""
                insert into return_requests(request_code, order_id, customer_id, store_id, request_type, status, reason_code, refund_amount)
                values (?, ?, ?, ?, 'RETURN', 'COMPLETED', 'SIZE', 100)""", "ZMR" + tag, o2a, customerY, store);
    }

    @Test
    void kpiCountsEachTopLevelOrderOnceAndExcludesCancelledAndEarlyOrders() {
        var report = reports.performance(admin, campaignA);
        Kpi k = report.kpi();
        assertThat(k.orders()).isEqualTo(3);
        assertThat(k.revenue()).isEqualByComparingTo("1000");
        assertThat(k.completedRevenue()).isEqualByComparingTo("800");
        assertThat(k.refunds()).isEqualByComparingTo("100");
        assertThat(k.netRevenue()).isEqualByComparingTo("700");
        assertThat(k.buyers()).isEqualTo(2);
        assertThat(k.firstTimeBuyers()).as("X đã có đơn o5 trước đó").isEqualTo(1);
        assertThat(k.onlineOrders()).isEqualTo(2);
        assertThat(k.offlineOrders()).isEqualTo(1);
        assertThat(k.discountCost()).as("chỉ khuyến mãi của A: 50 + 30").isEqualByComparingTo("80");
        assertThat(k.flashDiscountCost()).isEqualByComparingTo("40");
        assertThat(k.expenseCost()).isEqualByComparingTo("60");
        assertThat(k.totalCost()).isEqualByComparingTo("180");
        assertThat(k.roas()).isEqualByComparingTo("3.89");
        assertThat(k.budgetUsagePercent()).isZero(); // 180 / 60.000
        assertThat(k.averageOrderValue()).isEqualByComparingTo("333");

        assertThat(report.daily().stream().mapToLong(MarketingReportDtos.DayRow::orders).sum()).isEqualTo(3);
        assertThat(report.offers()).anySatisfy(o -> {
            assertThat(o.type()).isEqualTo("PROMOTION");
            assertThat(o.orders()).isEqualTo(2);
            assertThat(o.discount()).isEqualByComparingTo("80");
        });
        assertThat(report.offers()).anySatisfy(o -> {
            assertThat(o.type()).isEqualTo("FLASH_SALE");
            assertThat(o.orders()).isEqualTo(1);
        });
        assertThat(report.dataNotes()).anyMatch(n -> n.contains("KHÔNG phải doanh thu tăng thêm"));
        assertThat(report.dataNotes()).anyMatch(n -> n.contains("ước tính"));
    }

    @Test
    void comparisonCountsSharedOrdersOnce() {
        var comparison = reports.compare(admin, List.of(campaignA, campaignB));
        assertThat(comparison.rows()).extracting(MarketingReportDtos.CompareRow::orders).containsExactly(3L, 1L);
        assertThat(comparison.distinctOrders()).as("o2 dùng ưu đãi của cả A và B").isEqualTo(3);
        assertThat(comparison.distinctRevenue()).isEqualByComparingTo("1000");
        assertThat(comparison.overlaps()).singleElement().satisfies(o -> assertThat(o.sharedOrders()).isEqualTo(1));
        assertStatus(() -> reports.compare(admin, List.of(campaignA)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void calendarSeparatesTimelinesAndWarnsOnOverlaps() {
        jdbc.update("""
                insert into marketing_dispatches(campaign_id, code, title, message, criteria, scheduled_at, status)
                values (?, ?, 'Đợt 1', 'x', '{"version":1}'::jsonb, (now() at time zone 'UTC') + interval '1 day', 'SCHEDULED'),
                       (?, ?, 'Đợt 2', 'y', '{"version":1}'::jsonb, (now() at time zone 'UTC') + interval '1 day 2 hours', 'SCHEDULED')""",
                campaignA, "ZMRA" + tag, campaignB, "ZMRB" + tag);
        LocalDate today = LocalDate.now();
        var calendar = reports.calendar(admin, today.minusDays(3), today.plusDays(3));
        assertThat(calendar.events()).extracting(MarketingReportDtos.CalendarEvent::kind).contains("CAMPAIGN", "OFFER", "DISPATCH");
        assertThat(calendar.conflicts()).anyMatch(c -> c.contains("cùng hiệu lực"));
        assertThat(calendar.conflicts()).anyMatch(c -> c.contains("cách nhau dưới"));

        // Nhân viên cửa hàng: chỉ chiến dịch đã duyệt, không có đợt gửi và cảnh báo; không xem được báo cáo.
        jdbc.update("update marketing_campaigns set status = 'DRAFT' where id = ?", campaignB);
        var forStore = reports.calendar(storeStaff, today.minusDays(3), today.plusDays(3));
        assertThat(forStore.events()).extracting(MarketingReportDtos.CalendarEvent::campaignId).contains(campaignA).doesNotContain(campaignB);
        assertThat(forStore.events()).noneMatch(e -> e.kind().equals("DISPATCH"));
        assertThat(forStore.conflicts()).isEmpty();
        assertStatus(() -> reports.performance(storeStaff, campaignA), HttpStatus.FORBIDDEN);
        assertStatus(() -> reports.calendar(admin, today, today.plusDays(100)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void roiUsesWeightedReceiptCostOfCompletedOrders() {
        Map<String, Object> variant = jdbc.queryForMap("select id, product_id from product_variants limit 1");
        UUID supplier = jdbc.queryForObject("select id from suppliers limit 1", UUID.class);
        UUID receipt = UUID.randomUUID();
        jdbc.update("insert into goods_receipts(id, receipt_code, store_id, status, supplier_id) values (?, ?, ?, 'COMPLETED', ?)",
                receipt, "ZMRGR" + tag, store, supplier);
        jdbc.update("""
                insert into goods_receipt_items(receipt_id, product_variant_id, product_id, cost_price, quantity, total, target_channel)
                values (?, ?, ?, 50, 10, 500, 'OFFLINE')""", receipt, variant.get("id"), variant.get("product_id"));
        // o1 (hoàn tất) bán 3 món của biến thể này.
        jdbc.update("insert into order_items(order_id, product_id, product_variant_id, product_name, price, quantity, total) values (?, ?, ?, 'Zmr', 150, 3, 450)",
                o1, variant.get("product_id"), variant.get("id"));
        BigDecimal unitCost = jdbc.queryForObject("""
                select sum(ri.total) / sum(ri.quantity) from goods_receipt_items ri join goods_receipts gr on gr.id = ri.receipt_id
                where gr.status = 'COMPLETED' and ri.product_variant_id = ?""", BigDecimal.class, variant.get("id"));
        BigDecimal expectedCogs = unitCost.multiply(BigDecimal.valueOf(3)).setScale(0, java.math.RoundingMode.HALF_UP);

        Kpi k = reports.performance(admin, campaignA).kpi();
        assertThat(k.cogs()).isEqualByComparingTo(expectedCogs);
        assertThat(k.grossProfit()).isEqualByComparingTo(k.netRevenue().subtract(expectedCogs));
        assertThat(k.roi()).isEqualByComparingTo(k.grossProfit().subtract(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void storeCampaignCountsOnlyOrdersOfThatStore() {
        UUID otherStore = UUID.randomUUID();
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, ?, true, ?, ?)",
                otherStore, "ZMRO" + tag, "Zmr Other", 10 + Math.random(), 100 + Math.random());
        jdbc.update("update marketing_campaigns set store_id = ? where id = ?", store, campaignA);
        jdbc.update("update orders set store_id = ? where id = ?", store, o1);
        jdbc.update("update orders set store_id = ? where id = ?", otherStore, o3);
        // Đơn tách o2: một phần giao từ cửa hàng này → được tính.
        jdbc.update("update orders set store_id = ? where id = ?", store, o2a);
        jdbc.update("update orders set store_id = ? where id = ?", otherStore, o2b);

        var report = reports.performance(admin, campaignA);
        assertThat(report.kpi().orders()).isEqualTo(2);
        assertThat(report.storeId()).isEqualTo(store);
        assertThat(report.dataNotes()).anyMatch(n -> n.contains("chỉ tính đơn có phần thuộc cửa hàng này"));
    }

    @Test
    void holdoutExperimentUsesTwoProportionZTest() {
        var e = MarketingReportService.experiment(UUID.randomUUID(), "MD-1", "IN_APP", 50,
                new long[] {1000, 120, 130}, new long[] {1000, 80, 85}, BigDecimal.valueOf(130_000_000), BigDecimal.valueOf(85_000_000),
                null, null);
        assertThat(e.treatedRate()).isEqualTo(12.0);
        assertThat(e.holdoutRate()).isEqualTo(8.0);
        assertThat(e.upliftPoints()).isEqualTo(4.0);
        assertThat(e.zScore()).isEqualTo(2.98);
        assertThat(e.significant()).isTrue();
        assertThat(e.incrementalOrders()).isEqualByComparingTo("45.0");
        assertThat(e.incrementalRevenue()).isEqualByComparingTo("45000000");

        var small = MarketingReportService.experiment(UUID.randomUUID(), "MD-2", "IN_APP", 50,
                new long[] {20, 10, 10}, new long[] {20, 2, 2}, BigDecimal.TEN, BigDecimal.ONE, null, null);
        assertThat(small.significant()).as("dưới 30 người mỗi nhóm thì chưa kết luận").isFalse();
    }

    @Test
    void exportProducesAWorkbook() {
        byte[] file = reports.exportPerformance(admin, campaignA);
        assertThat(file.length).isGreaterThan(1000);
        assertThat(new String(file, 0, 2)).isEqualTo("PK");
    }

    // ------------------------------------------------------------------ dữ liệu mẫu

    private void campaign(UUID id, String suffix, int budget) {
        jdbc.update("""
                insert into marketing_campaigns(id, code, name, objective, starts_at, ends_at, status, planned_budget, created_by)
                values (?, ?, ?, 'REVENUE', (now() at time zone 'UTC') - interval '10 day', (now() at time zone 'UTC') + interval '10 day',
                        'APPROVED', ?, ?)""", id, "ZMR" + suffix + tag, "Zmr " + suffix, budget == 0 ? null : BigDecimal.valueOf(budget), admin);
    }

    private void promotion(UUID id) {
        jdbc.update("""
                insert into promotions(id, code, name, promotion_type, discount_type, discount_value, start_date, end_date, status, created_at)
                values (?, ?, 'Zmr promo', 'ORDER', 'PERCENT', 10, (now() at time zone 'UTC') - interval '5 day',
                        (now() at time zone 'UTC') + interval '5 day', 'ACTIVE', (now() at time zone 'UTC'))""",
                id, ("ZMR" + id.toString().substring(0, 8)).toUpperCase());
    }

    private void link(UUID campaign, String type, String column, UUID item) {
        jdbc.update("insert into marketing_campaign_offers(campaign_id, offer_type, " + column + ") values (?, ?, ?)", campaign, type, item);
    }

    private void order(UUID id, UUID parent, String type, String status, int total, UUID customer, String ago) {
        jdbc.update("""
                insert into orders(id, order_code, order_type, status, total_amount, shipping_fee, customer_id, parent_order_id,
                                   created_at, active)
                values (?, ?, ?, ?, ?, 0, ?, ?, (now() at time zone 'UTC') - cast(? as interval), true)""",
                id, "ZMR" + tag + (codes++), type, status, BigDecimal.valueOf(total), customer, parent, ago);
    }

    private void promoUse(UUID order, UUID promotion, int discount) {
        jdbc.update("insert into order_promotions(order_id, promotion_id, discount_amount) values (?, ?, ?)", order, promotion, BigDecimal.valueOf(discount));
    }

    private void customer(UUID id, String suffix) {
        jdbc.update("""
                insert into customers(id, customer_code, full_name, active, membership_status, source, created_at)
                values (?, ?, ?, true, 'MEMBER', 'WEBSITE', (now() at time zone 'UTC'))""", id, "ZMR" + tag + suffix, "Zmr " + suffix);
    }

    private void user(UUID id, String name, String role, UUID storeId) {
        jdbc.update("insert into users(id, username, full_name, active, locked) values (?, ?, ?, true, false)", id, "zmr-" + name + "-" + tag, "Zmr " + name);
        jdbc.update("insert into user_roles(user_id, role_id) select ?, id from roles where code = ?", id, role);
        if (storeId != null) jdbc.update("insert into store_staffs(user_id, store_id, active) values (?, ?, true)", id, storeId);
    }

    private static void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }
}
