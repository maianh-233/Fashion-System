package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.StocktakeDtos;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/** Kiểm kê trên cơ sở dữ liệu thật; phạm vi cửa hàng được giả lập, dữ liệu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class StocktakeIntegrationTest {
    @Autowired StocktakeService stocktakes;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean StoreAccessService storeAccess;

    final UUID manager = UUID.randomUUID();
    final UUID counter = UUID.randomUUID();
    final UUID store = UUID.randomUUID();
    final UUID parentCat = UUID.randomUUID();
    final UUID childCat = UUID.randomUUID();
    final UUID otherCat = UUID.randomUUID();
    final UUID shirt = UUID.randomUUID();
    final UUID jeans = UUID.randomUUID();
    final UUID shirtM = UUID.randomUUID();
    final UUID shirtL = UUID.randomUUID();
    final UUID jeans30 = UUID.randomUUID();
    String tag;

    @BeforeEach
    void setUp() {
        tag = store.toString().substring(0, 8);
        jdbc.update("insert into users(id, username, full_name) values (?, ?, 'Zqk Quản lý')", manager, "zqk-m-" + tag);
        jdbc.update("insert into users(id, username, full_name) values (?, ?, 'Zqk Nhân viên')", counter, "zqk-c-" + tag);
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, 'Zqk Store', true)", store, "ZQK-" + tag);
        jdbc.update("insert into categories(id, name, code, active) values (?, 'Zqk Áo', ?, true)", parentCat, "ZQKA" + tag);
        jdbc.update("insert into categories(id, name, code, parent_id, active) values (?, 'Zqk Áo sơ mi', ?, ?, true)", childCat, "ZQKS" + tag, parentCat);
        jdbc.update("insert into categories(id, name, code, active) values (?, 'Zqk Quần', ?, true)", otherCat, "ZQKQ" + tag);
        product(shirt, childCat, "so-mi");
        product(jeans, otherCat, "jeans");
        variant(shirtM, shirt, "M", "8930000" + tag.hashCode() % 1000 + "1");
        variant(shirtL, shirt, "L", "8930000" + tag.hashCode() % 1000 + "2");
        variant(jeans30, jeans, "30", "8930000" + tag.hashCode() % 1000 + "3");
        balance(shirtM, 10, 2);    // tồn kiểm 12
        balance(shirtL, 3, 0);
        balance(jeans30, 5, 0);
        receiptCost(shirtM, new BigDecimal("120000"));
        for (UUID actor : new UUID[]{manager, counter}) {
            when(storeAccess.require(eq(actor), any(), anyString())).thenAnswer(call -> call.getArgument(1));
            when(storeAccess.requireOrAll(eq(actor), any(), anyString())).thenAnswer(call -> call.getArgument(1));
            when(storeAccess.allows(eq(actor), any(), anyString())).thenReturn(true);
        }
        when(storeAccess.allows(eq(counter), any(), eq("STOCKTAKE_APPROVE"))).thenReturn(false);
    }

    @Test
    void categoryScopeIncludesSubcategoriesAndScanCountsByBarcodeOrSku() {
        StocktakeDtos.Stocktake st = job(() -> stocktakes.create(counter, new StocktakeDtos.CreateRequest(store, parentCat, false, null)));
        assertThat(st.code()).startsWith("KK-");
        assertThat(st.summary().itemCount()).isEqualTo(2);                 // hai size áo, không gồm quần
        assertThat(st.summary().systemQuantity()).isEqualTo(15);

        String barcodeM = barcode(shirtM);
        StocktakeDtos.ScanResult first = job(() -> stocktakes.scan(counter, st.id(), new StocktakeDtos.ScanRequest(barcodeM, null)));
        assertThat(first.item().countedQuantity()).isEqualTo(1);
        job(() -> stocktakes.scan(counter, st.id(), new StocktakeDtos.ScanRequest("zqk-" + tag + "-m", 9)));   // SKU, không phân biệt hoa thường
        assertThat(job(() -> stocktakes.scan(counter, st.id(), new StocktakeDtos.ScanRequest(barcodeM, null))).item().countedQuantity())
                .isEqualTo(11);
        assertThatThrownBy(() -> job(() -> stocktakes.scan(counter, st.id(), new StocktakeDtos.ScanRequest(barcode(jeans30), null))))
                .hasMessageContaining("không thuộc danh mục");
        assertThatThrownBy(() -> job(() -> stocktakes.scan(counter, st.id(), new StocktakeDtos.ScanRequest("KHONG-CO-" + tag, null))))
                .hasMessageContaining("Không tìm thấy");
        assertThatThrownBy(() -> job(() -> stocktakes.create(manager, new StocktakeDtos.CreateRequest(store, null, false, null))))
                .hasMessageContaining("chưa hoàn tất");
    }

    @Test
    void approvalAppliesVarianceToCurrentStockAndCreatesAdjustment() {
        StocktakeDtos.Stocktake st = job(() -> stocktakes.create(counter, new StocktakeDtos.CreateRequest(store, null, false, "Cuối tháng")));
        UUID itemM = itemId(st.id(), shirtM);
        UUID itemL = itemId(st.id(), shirtL);
        job(() -> stocktakes.setCount(counter, st.id(), itemM, new StocktakeDtos.CountRequest(9, "Thiếu 3")));    // 12 → 9
        job(() -> stocktakes.setCount(counter, st.id(), itemL, new StocktakeDtos.CountRequest(4, null)));         // 3 → 4
        // Quần chưa đếm: bỏ qua. Trong lúc đếm bán thêm 2 áo M tại quầy.
        jdbc.update("update inventory_balances set available_quantity = available_quantity - 2 where store_id = ? and product_variant_id = ?", store, shirtM);

        assertThatThrownBy(() -> job(() -> stocktakes.approve(manager, st.id(), null))).hasMessageContaining("chờ duyệt");
        StocktakeDtos.Stocktake submitted = job(() -> stocktakes.submit(counter, st.id(), new StocktakeDtos.SubmitRequest(false)));
        assertThat(submitted.status()).isEqualTo("SUBMITTED");
        assertThat(submitted.summary().shortageQuantity()).isEqualTo(3);
        assertThat(submitted.summary().surplusQuantity()).isEqualTo(1);
        assertThatThrownBy(() -> job(() -> stocktakes.setCount(counter, st.id(), itemM, new StocktakeDtos.CountRequest(1, null))))
                .hasMessageContaining("bước đếm");

        StocktakeDtos.Stocktake approved = job(() -> stocktakes.approve(manager, st.id(), new StocktakeDtos.Decision("OK")));
        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(approved.adjustment().code()).startsWith("DCK-");
        assertThat(approved.adjustment().decreaseQuantity()).isEqualTo(3);
        assertThat(approved.adjustment().increaseQuantity()).isEqualTo(1);
        assertThat(approved.adjustment().decreaseValue()).isEqualByComparingTo("360000");      // 3 × giá nhập 120.000
        // Áo M: 8 tại quầy + 2 online (sau bán) − 3 = 7 (trừ tại quầy trước); áo L +1 tại quầy; quần giữ nguyên.
        assertThat(stock(shirtM)).containsExactly(5, 2);
        assertThat(stock(shirtL)).containsExactly(4, 0);
        assertThat(stock(jeans30)).containsExactly(5, 0);
        assertThat(jdbc.queryForObject("select count(*) from inventory_transactions where reference_type = 'STOCK_ADJUSTMENT'"
                + " and reference_id = ? and transaction_type = 'STOCKTAKE_ADJUST'", Integer.class, approved.adjustment().id())).isEqualTo(2);

        StocktakeDtos.Report report = stocktakes.report(manager, store, LocalDate.now().minusDays(1), LocalDate.now());
        assertThat(report.totals().stocktakes()).isEqualTo(1);
        assertThat(report.totals().shortageQuantity()).isEqualTo(3);
        assertThat(report.totals().shortageValue()).isEqualByComparingTo("360000");
        assertThat(report.topShortages()).singleElement().satisfies(v -> assertThat(v.variantId()).isEqualTo(shirtM));
        assertThat(report.categories()).extracting(StocktakeDtos.CategoryRow::categoryName).contains("Zqk Áo sơ mi");
    }

    @Test
    void zeroUncountedWritesOffMissingItemsAndShortageIsCappedByCurrentStock() {
        StocktakeDtos.Stocktake st = job(() -> stocktakes.create(counter, new StocktakeDtos.CreateRequest(store, null, false, null)));
        job(() -> stocktakes.setCount(counter, st.id(), itemId(st.id(), shirtM), new StocktakeDtos.CountRequest(12, null)));
        jdbc.update("update inventory_balances set available_quantity = 1 where store_id = ? and product_variant_id = ?", store, jeans30);
        job(() -> stocktakes.submit(counter, st.id(), new StocktakeDtos.SubmitRequest(true)));
        StocktakeDtos.Stocktake approved = job(() -> stocktakes.approve(manager, st.id(), null));
        // Áo L 3 → 0; quần chụp 5 nhưng hiện chỉ còn 1 → chỉ trừ được 1.
        assertThat(stock(shirtL)).containsExactly(0, 0);
        assertThat(stock(jeans30)).containsExactly(0, 0);
        assertThat(approved.adjustment().decreaseQuantity()).isEqualTo(4);
        assertThat(jdbc.queryForObject("select adjusted_quantity from stocktake_items where stocktake_id = ? and product_variant_id = ?",
                Integer.class, st.id(), jeans30)).isEqualTo(-1);
    }

    @Test
    void blindCountHidesSystemQuantityFromCountersOnly() {
        StocktakeDtos.Stocktake st = job(() -> stocktakes.create(counter, new StocktakeDtos.CreateRequest(store, null, true, null)));
        assertThat(st.systemVisible()).isFalse();
        assertThat(st.summary().systemQuantity()).isNull();
        assertThat(stocktakes.items(counter, st.id(), "ALL", null, Pageable.ofSize(50)).getContent())
                .allSatisfy(i -> assertThat(i.systemQuantity()).isNull());
        assertThat(stocktakes.items(manager, st.id(), "ALL", null, Pageable.ofSize(50)).getContent())
                .allSatisfy(i -> assertThat(i.systemQuantity()).isNotNull());
        assertThat(stocktakes.detail(manager, st.id()).summary().systemQuantity()).isEqualTo(20);
    }

    @Test
    void rejectReturnsToCountingAndCancelReleasesStore() {
        StocktakeDtos.Stocktake st = job(() -> stocktakes.create(counter, new StocktakeDtos.CreateRequest(store, null, false, null)));
        assertThatThrownBy(() -> job(() -> stocktakes.submit(counter, st.id(), null))).hasMessageContaining("Chưa đếm");
        job(() -> stocktakes.scan(counter, st.id(), new StocktakeDtos.ScanRequest(barcode(shirtL), 3)));
        job(() -> stocktakes.submit(counter, st.id(), null));
        StocktakeDtos.Stocktake back = job(() -> stocktakes.reject(manager, st.id(), new StocktakeDtos.Rejection("Đếm lại quầy 2")));
        assertThat(back.status()).isEqualTo("IN_PROGRESS");
        assertThat(back.rejectionReason()).isEqualTo("Đếm lại quầy 2");
        assertThat(job(() -> stocktakes.cancel(counter, st.id())).status()).isEqualTo("CANCELLED");
        assertThat(job(() -> stocktakes.create(manager, new StocktakeDtos.CreateRequest(store, null, false, null))).status())
                .isEqualTo("IN_PROGRESS");
    }

    private void product(UUID id, UUID category, String slug) {
        jdbc.update("insert into products(id, code, name, slug, status, image_url, category_id) values (?, ?, ?, ?, 'ACTIVE', '', ?)",
                id, "ZQK-" + slug + tag, "Zqk " + slug, "zqk-" + slug + "-" + tag, category);
    }

    private void variant(UUID id, UUID product, String size, String barcode) {
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, barcode, active) values (?, ?, ?, 'Trắng', ?, 250000, ?, true)",
                id, product, "ZQK-" + tag + "-" + size, size, barcode + id.toString().substring(0, 4));
    }

    private void balance(UUID variant, int counter, int online) {
        jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, ?, ?, 0)",
                store, variant, counter, online);
    }

    private void receiptCost(UUID variant, BigDecimal cost) {
        UUID receipt = UUID.randomUUID();
        UUID supplier = jdbc.queryForObject("select id from suppliers limit 1", UUID.class);
        jdbc.update("insert into goods_receipts(id, receipt_code, store_id, supplier_id, status, completed_at) values (?, ?, ?, ?, 'COMPLETED', now())",
                receipt, "ZQK-PN-" + tag, store, supplier);
        UUID product = jdbc.queryForObject("select product_id from product_variants where id = ?", UUID.class, variant);
        jdbc.update("insert into goods_receipt_items(receipt_id, product_variant_id, product_id, cost_price, quantity, total, target_channel)"
                + " values (?, ?, ?, ?, 10, ?, 'OFFLINE')", receipt, variant, product, cost, cost.multiply(BigDecimal.TEN));
    }

    private String barcode(UUID variant) {
        return jdbc.queryForObject("select barcode from product_variants where id = ?", String.class, variant);
    }

    private UUID itemId(UUID stocktake, UUID variant) {
        return jdbc.queryForObject("select id from stocktake_items where stocktake_id = ? and product_variant_id = ?", UUID.class, stocktake, variant);
    }

    private Integer[] stock(UUID variant) {
        return jdbc.queryForObject("select available_quantity, online_quantity from inventory_balances where store_id = ? and product_variant_id = ?",
                (rs, i) -> new Integer[]{rs.getInt(1), rs.getInt(2)}, store, variant);
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_STOCKTAKE")) {
            return action.get();
        }
    }
}
