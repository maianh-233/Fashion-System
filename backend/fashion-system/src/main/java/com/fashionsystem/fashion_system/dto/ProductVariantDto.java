package com.fashionsystem.fashion_system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của ProductVariant giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDto {
    private UUID id;
    private UUID productId;
    private String sku;
    private String color;
    private String size;
    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal weight;
    private String barcode;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

