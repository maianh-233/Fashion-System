package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.storefront.SizeAdvisorDtos;
import com.fashionsystem.fashion_system.dto.storefront.SizeAdvisorDtos.ChartEntry;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Gợi ý size trên cơ sở dữ liệu thật; dữ liệu do test tạo và rollback sau mỗi test. */
@SpringBootTest
@Transactional
class SizeAdvisorIntegrationTest {
    @Autowired SizeAdvisorService sizes;
    @Autowired JdbcTemplate jdbc;

    final UUID parent = UUID.randomUUID();
    final UUID child = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID bought = UUID.randomUUID();
    final UUID boughtM = UUID.randomUUID();
    final UUID customer = UUID.randomUUID();
    final UUID store = UUID.randomUUID();
    String tag;

    @BeforeEach
    void setUp() {
        tag = product.toString().substring(0, 8);
        jdbc.update("insert into categories(id, name, code) values (?, 'Zqsz Áo', ?)", parent, "ZQSZ-P-" + tag);
        jdbc.update("insert into categories(id, parent_id, name, code) values (?, ?, 'Zqsz Áo thun', ?)", child, parent, "ZQSZ-C-" + tag);
        product(product, "zqsz-ao-thun-" + tag, "S", "M", "L");
        product(bought, "zqsz-ao-polo-" + tag, "L");
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Xanh', 'M', 300000, true)",
                boughtM, bought, "ZQSZ-" + tag + "-polo-M");
        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name) values (?, ?, 'MEMBER', 'WEBSITE', 'Zqsz Khách')",
                customer, "ZQSZ-" + tag);
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, 'Zqsz Store', true)", store, "ZQSZ-" + tag);
        asJob(() -> sizes.replaceChart(parent, new SizeAdvisorDtos.ChartRequest(List.of(
                new ChartEntry("S", 150, 160, new BigDecimal("40"), new BigDecimal("50")),
                new ChartEntry("M", 160, 170, new BigDecimal("50"), new BigDecimal("60")),
                new ChartEntry("L", 170, 180, new BigDecimal("60"), new BigDecimal("72"))))));
    }

    @Test
    void childCategoryUsesTheParentChartAndMeasurementsPickTheSize() {
        SizeAdvisorDtos.Guide guide = sizes.guide(product, null, 165, new BigDecimal("55"));
        assertThat(guide.chartCategoryName()).isEqualTo("Zqsz Áo");
        assertThat(guide.entries()).extracting(ChartEntry::size).containsExactly("S", "M", "L");
        assertThat(guide.productSizes()).containsExactly("S", "M", "L");
        assertThat(guide.recommendedSize()).isEqualTo("M");
        assertThat(guide.source()).isEqualTo(SizeAdvisorDtos.SOURCE_CHART);

        // Cao gần size L, nặng hơi quá size M: lệch 1 cm (L) ít hơn lệch 1 kg (M).
        assertThat(sizes.guide(product, null, 169, new BigDecimal("61")).recommendedSize()).isEqualTo("L");
        // Nằm giữa hai size thì chọn size lớn hơn.
        assertThat(sizes.guide(product, null, 160, null).recommendedSize()).isEqualTo("M");
        assertThat(sizes.guide(product, null, null, null).recommendedSize()).isNull();
        assertThat(sizes.guide(product, null, null, null).note()).contains("Nhập chiều cao");
    }

    @Test
    void keptPurchaseInTheSameCategoryWinsOverTheChartAndReturnsAreIgnored() {
        asJob(() -> sizes.saveMeasurements(customer, new SizeAdvisorDtos.Measurements(158, new BigDecimal("45"))));
        assertThat(sizes.guide(product, customer, null, null)).satisfies(guide -> {
            assertThat(guide.heightCm()).isEqualTo(158);
            assertThat(guide.recommendedSize()).isEqualTo("S");
        });

        UUID orderItem = completedPurchase(UUID.fromString(jdbc.queryForObject(
                "select id::text from product_variants where product_id = ? and size = 'L'", String.class, bought)), "L");
        assertThat(sizes.guide(product, customer, null, null)).satisfies(guide -> {
            assertThat(guide.historySize()).isEqualTo("L");
            assertThat(guide.recommendedSize()).isEqualTo("L");
            assertThat(guide.source()).isEqualTo(SizeAdvisorDtos.SOURCE_HISTORY);
            assertThat(guide.chartSize()).isEqualTo("S");
        });

        // Đổi sang size M đã hoàn tất: tính size M; trả hàng thì món đó không được tính.
        UUID request = returnRequest(orderItem, "EXCHANGE", "COMPLETED", boughtM);
        assertThat(sizes.guide(product, customer, null, null).historySize()).isEqualTo("M");
        jdbc.update("update return_requests set request_type = 'RETURN' where id = ?", request);
        jdbc.update("update return_request_items set exchange_variant_id = null where request_id = ?", request);
        assertThat(sizes.guide(product, customer, null, null)).satisfies(guide -> {
            assertThat(guide.historySize()).isNull();
            assertThat(guide.recommendedSize()).isEqualTo("S");
        });
    }

    @Test
    void chartValidationRejectsDuplicatesAndInvertedRanges() {
        assertThatThrownBy(() -> asJob(() -> sizes.replaceChart(child, new SizeAdvisorDtos.ChartRequest(List.of(
                new ChartEntry("M", 160, 170, null, null), new ChartEntry(" m ", 170, 180, null, null))))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("trùng");
        assertThatThrownBy(() -> asJob(() -> sizes.replaceChart(child, new SizeAdvisorDtos.ChartRequest(List.of(
                new ChartEntry("M", 170, 160, null, null))))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("nhỏ hơn");

        // Bảng riêng của danh mục con thay cho bảng của danh mục cha; bảng rỗng = quay lại dùng bảng cha.
        asJob(() -> sizes.replaceChart(child, new SizeAdvisorDtos.ChartRequest(List.of(new ChartEntry("M", 150, 190, null, null)))));
        assertThat(sizes.guide(product, null, 155, null).chartCategoryName()).isEqualTo("Zqsz Áo thun");
        asJob(() -> sizes.replaceChart(child, new SizeAdvisorDtos.ChartRequest(List.of())));
        assertThat(sizes.guide(product, null, 155, null).chartCategoryName()).isEqualTo("Zqsz Áo");
    }

    @Test
    void sizesSortByChartThenLetterThenNumber() {
        List<ChartEntry> chart = List.of(new ChartEntry("Nhỏ", 1, 2, null, null), new ChartEntry("Lớn", 3, 4, null, null));
        assertThat(SizeAdvisorService.sortSizes(List.of("XL", "Lớn", "S", "32", "28", "Nhỏ"), chart))
                .containsExactly("Nhỏ", "Lớn", "S", "XL", "28", "32");
    }

    private UUID completedPurchase(UUID variant, String size) {
        UUID order = UUID.randomUUID();
        jdbc.update("insert into orders(id, order_code, order_type, status, customer_id, store_id) values (?, ?, 'OFFLINE', 'COMPLETED', ?, ?)",
                order, "ZQSZ-" + tag, customer, store);
        UUID item = UUID.randomUUID();
        jdbc.update("insert into order_items(id, order_id, product_variant_id, product_name, size, price, quantity, total) "
                + "values (?, ?, ?, 'Zqsz Polo', ?, 300000, 1, 300000)", item, order, variant, size);
        return item;
    }

    private UUID returnRequest(UUID orderItem, String type, String status, UUID exchangeVariant) {
        UUID request = UUID.randomUUID();
        UUID order = UUID.fromString(jdbc.queryForObject("select order_id::text from order_items where id = ?", String.class, orderItem));
        jdbc.update("insert into return_requests(id, request_code, order_id, customer_id, store_id, request_type, status, reason_code) "
                + "values (?, ?, ?, ?, ?, ?, ?, 'WRONG_SIZE')", request, "ZQSZ-" + tag, order, customer, store, type, status);
        jdbc.update("insert into return_request_items(request_id, order_item_id, quantity, unit_price, exchange_variant_id) values (?, ?, 1, 300000, ?)",
                request, orderItem, exchangeVariant);
        return request;
    }

    private void product(UUID id, String slug, String... sizesOfProduct) {
        jdbc.update("insert into products(id, code, name, slug, status, image_url, category_id) values (?, ?, ?, ?, 'ACTIVE', '', ?)",
                id, slug.toUpperCase(), slug, slug, child);
        for (String size : sizesOfProduct)
            jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Đen', ?, 300000, true)",
                    UUID.randomUUID(), id, slug + "-" + size, size);
    }

    private <T> T asJob(java.util.function.Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_SIZE_CHART")) {
            return action.get();
        }
    }
}
