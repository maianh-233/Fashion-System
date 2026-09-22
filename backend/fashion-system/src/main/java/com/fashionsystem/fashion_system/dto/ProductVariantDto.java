package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private String productName;
    private String productCode;
    private String imageUrl;
    @NotBlank
    @Size(max = 100)
    private String sku;
    @Size(max = 100)
    private String color;
    @Size(max = 50)
    private String size;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;
    @DecimalMin("0.00")
    private BigDecimal salePrice;
    @DecimalMin("0.00")
    private BigDecimal weight;
    @Size(max = 100)
    private String barcode;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
