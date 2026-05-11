package com.fashion.inventoryservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionResponse {

    private UUID id;

    private UUID productVariantId;

    private UUID storeId;

    private String transactionType;

    private String referenceType;

    private UUID referenceId;

    private Integer quantity;

    private Integer balanceAfter;

    private LocalDateTime createdAt;
}