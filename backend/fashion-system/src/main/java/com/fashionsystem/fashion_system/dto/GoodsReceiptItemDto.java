package com.fashionsystem.fashion_system.dto;

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
    private UUID productVariantId;
    private String sku;
    private String productName;
    private BigDecimal costPrice;
    private Integer quantity;
    private BigDecimal total;
    private LocalDateTime createdAt;
}

