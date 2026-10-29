package com.fashionsystem.fashion_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Điều chuyển tồn tại quầy giữa các cửa hàng và danh sách hàng sắp hết kèm gợi ý nơi có dư. */
public final class StockTransferDtos {
    public static final Map<String, String> STATUS_LABELS = Map.of(
            "REQUESTED", "Chờ cửa hàng gửi duyệt",
            "SHIPPED", "Đang chuyển",
            "RECEIVED", "Đã nhận",
            "REJECTED", "Bị từ chối",
            "CANCELLED", "Đã hủy");

    private StockTransferDtos() {
    }

    public record CreateItem(@NotNull UUID variantId, @NotNull @Min(1) Integer quantity) {
    }

    /** Cửa hàng nhận ({@code toStoreId}, mặc định cửa hàng của nhân viên) xin hàng từ {@code fromStoreId}. */
    public record CreateRequest(@NotNull UUID fromStoreId, UUID toStoreId, @Size(max = 1000) String note,
                                @NotEmpty @Size(max = 100) List<@Valid @NotNull CreateItem> items) {
    }

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {
    }

    /** Số thực xuất theo biến thể; bỏ trống là xuất đủ số yêu cầu. */
    public record ShipRequest(Map<UUID, @Min(0) Integer> quantities) {
    }

    public record StoreRef(UUID id, String name) {
    }

    /** {@code sourceStock}: tồn tại quầy hiện tại của cửa hàng gửi. */
    public record Item(UUID variantId, String sku, String productName, String color, String size, int requested,
                       Integer shipped, int sourceStock) {
    }

    public record Transfer(UUID id, String transferCode, StoreRef fromStore, StoreRef toStore, String status,
                           String statusLabel, String note, String rejectReason, List<Item> items,
                           LocalDateTime createdAt, LocalDateTime shippedAt, LocalDateTime receivedAt) {
    }

    /**
     * Biến thể sắp hết ở cửa hàng ({@code total} = tại quầy + online ≤ ngưỡng). {@code incoming}: số đang yêu cầu /
     * đang chuyển tới. Gợi ý: cửa hàng khác có tồn tại quầy nhiều nhất (vượt ngưỡng).
     */
    public record LowStock(UUID variantId, UUID productId, String sku, String productName, String color, String size,
                           int counter, int online, int total, int incoming, StoreRef suggestedStore,
                           Integer suggestedStock) {
    }
}
