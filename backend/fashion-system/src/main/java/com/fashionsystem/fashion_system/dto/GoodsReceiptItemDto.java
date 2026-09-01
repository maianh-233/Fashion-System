package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của GoodsReceiptItem giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItemDto {
    private UUID id;
    private UUID receiptId;
    @NotNull
    private UUID productVariantId;
    private String sku;
    private String productName;
    @NotNull @DecimalMin("0.00")
    private BigDecimal costPrice;
    @NotNull @Min(1)
    private Integer quantity;
    private BigDecimal total;
    private LocalDateTime createdAt;
}
