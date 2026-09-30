package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của InventoryTransaction giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionDto {
    private UUID id;
    private UUID productVariantId;
    private UUID storeId;
    private String transactionType;
    private String referenceType;
    private UUID referenceId;
    private Integer quantity;
    private Integer balanceAfter;
    private Integer beforeOffline;
    private Integer afterOffline;
    private Integer beforeOnline;
    private Integer afterOnline;
    private String fromChannel;
    private String toChannel;
    private UUID importReceiptId;
    private UUID exportReceiptId;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
