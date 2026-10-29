package com.fashionsystem.fashion_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Flash sale: giá flash theo biến thể trong khung giờ, tổng số suất, tối đa {@value #PER_CUSTOMER} món / khách / biến thể. */
public final class FlashSaleDtos {
    public static final int PER_CUSTOMER = 2;
    public static final String LINE_LABEL = "FLASH SALE";

    private FlashSaleDtos() {
    }

    public record ItemRequest(@NotNull UUID variantId,
                              @NotNull @DecimalMin(value = "1", message = "Giá flash phải lớn hơn 0") BigDecimal salePrice,
                              @NotNull @Min(1) Integer quota) {
    }

    public record SaleRequest(@NotBlank @Size(max = 150) String name, @NotNull LocalDateTime startsAt,
                              @NotNull LocalDateTime endsAt, @NotEmpty @Size(max = 200) List<@Valid @NotNull ItemRequest> items) {
    }

    /** {@code sold}: suất đã dùng bởi đơn chưa hủy. */
    public record AdminItem(UUID variantId, UUID productId, String sku, String productName, String color, String size,
                            BigDecimal regularPrice, BigDecimal salePrice, int quota, int sold) {
    }

    /** {@code status}: UPCOMING / RUNNING / ENDED / STOPPED (đã tắt). */
    public record AdminSale(UUID id, String name, LocalDateTime startsAt, LocalDateTime endsAt, boolean active,
                            String status, List<AdminItem> items) {
    }

    /** Một biến thể đang flash sale, hiển thị trên trang chủ / trang sản phẩm. */
    public record LiveItem(UUID variantId, UUID productId, String productName, String color, String size, String imageUrl,
                           BigDecimal regularPrice, BigDecimal salePrice, int quota, int remaining) {
    }

    public record LiveSale(UUID id, String name, LocalDateTime startsAt, LocalDateTime endsAt, List<LiveItem> items) {
    }
}
