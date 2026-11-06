package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTO của kiểm kê kho, phiếu điều chỉnh và báo cáo hao hụt. */
public final class StocktakeDtos {
    private StocktakeDtos() {
    }

    public record Ref(UUID id, String name) {
    }

    /** Cửa hàng được chọn, danh mục (cây phẳng, {@code depth} để thụt lề) và quyền của người xem. */
    public record Lookups(List<Ref> stores, List<CategoryRef> categories, boolean canCount, boolean canApprove, boolean global,
                          UUID scopeStoreId) {
    }

    public record CategoryRef(UUID id, String name, UUID parentId, int depth) {
    }

    public record CreateRequest(@NotNull UUID storeId, UUID categoryId, boolean blindCount, @Size(max = 1000) String note) {
    }

    public record ScanRequest(@NotBlank @Size(max = 100) String code, @Min(1) @Max(9999) Integer quantity) {
    }

    public record CountRequest(@NotNull @Min(0) @Max(999999) Integer countedQuantity, @Size(max = 500) String note) {
    }

    public record SubmitRequest(boolean zeroUncounted) {
    }

    public record Decision(@Size(max = 1000) String note) {
    }

    public record Rejection(@NotBlank @Size(max = 1000) String reason) {
    }

    /** Tổng hợp tiến độ và chênh lệch; số / giá trị hệ thống là null khi phiếu đếm mù và người xem không duyệt được. */
    public record Summary(int itemCount, int countedCount, int varianceCount, Integer systemQuantity, int countedQuantity,
                          Integer shortageQuantity, Integer surplusQuantity, BigDecimal shortageValue, BigDecimal surplusValue) {
    }

    public record Adjustment(UUID id, String code, int increaseQuantity, int decreaseQuantity, BigDecimal increaseValue,
                             BigDecimal decreaseValue, int lineCount, String approvedByName, LocalDateTime createdAt) {
    }

    public record Stocktake(UUID id, String code, UUID storeId, String storeName, String scopeType, UUID categoryId,
                            String categoryName, boolean blindCount, String status, String note, boolean zeroUncounted,
                            String rejectionReason, String approverNote, String createdByName, String submittedByName,
                            String decidedByName, LocalDateTime createdAt, LocalDateTime submittedAt, LocalDateTime decidedAt,
                            Summary summary, Adjustment adjustment, boolean systemVisible, boolean canCount, boolean canApprove) {
    }

    /** {@code systemQuantity} / {@code variance} null khi bị ẩn (đếm mù). */
    public record Item(UUID id, UUID variantId, UUID productId, String sku, String barcode, String productName, String color,
                       String size, String categoryName, Integer systemQuantity, Integer countedQuantity, Integer variance,
                       BigDecimal unitCost, String costSource, Integer adjustedQuantity, boolean addedDuringCount, String note,
                       String countedByName, LocalDateTime countedAt) {
    }

    public record ScanResult(Item item, boolean added, String message) {
    }

    // ------------------------------------------------------------------ báo cáo hao hụt

    public record ReportTotals(int stocktakes, int itemsCounted, int systemQuantity, int countedQuantity, int shortageQuantity,
                               int surplusQuantity, BigDecimal systemValue, BigDecimal shortageValue, BigDecimal surplusValue,
                               BigDecimal netValue, BigDecimal shrinkageRate) {
    }

    public record StoreRow(UUID storeId, String storeName, int stocktakes, int shortageQuantity, int surplusQuantity,
                           BigDecimal shortageValue, BigDecimal surplusValue, BigDecimal netValue, BigDecimal shrinkageRate) {
    }

    public record CategoryRow(UUID categoryId, String categoryName, int shortageQuantity, int surplusQuantity,
                              BigDecimal shortageValue, BigDecimal surplusValue, BigDecimal netValue) {
    }

    public record VariantRow(UUID variantId, String sku, String productName, String color, String size, int shortageQuantity,
                             BigDecimal shortageValue, int occurrences) {
    }

    public record MonthRow(String month, BigDecimal shortageValue, BigDecimal surplusValue, BigDecimal netValue) {
    }

    public record Report(LocalDate from, LocalDate to, UUID storeId, ReportTotals totals, List<StoreRow> stores,
                         List<CategoryRow> categories, List<VariantRow> topShortages, List<MonthRow> months) {
    }
}
