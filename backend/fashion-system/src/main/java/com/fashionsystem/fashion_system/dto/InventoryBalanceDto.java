package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của InventoryBalance giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBalanceDto {
    private UUID storeId;
    private UUID productVariantId;
    private Integer availableQuantity;
    private Integer offlineQuantity;
    private Integer onlineQuantity;
    private Integer totalQuantity;
    private UUID productId;
    private UUID categoryId;
    private String productName;
    private String sku;
    private String variantName;
    private Integer reservedQuantity;
    private Integer damagedQuantity;
    private LocalDateTime updatedAt;
}
