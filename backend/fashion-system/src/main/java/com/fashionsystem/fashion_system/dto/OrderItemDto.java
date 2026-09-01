package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO dùng để truyền dữ liệu chi tiết sản phẩm trong đơn hàng giữa các tầng ứng dụng. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemDto {
    private UUID id;
    private UUID orderId;
    private UUID productId;
    @NotNull
    private UUID productVariantId;
    private String productName;
    private String sku;
    private String color;
    private String size;
    private String imageUrl;
    private BigDecimal price;
    @NotNull @Min(1)
    private Integer quantity;
    private BigDecimal total;
    private LocalDateTime createdAt;
}
