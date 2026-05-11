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
public class InventoryBalanceResponse {

    private UUID storeId;

    private UUID productVariantId;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Integer damagedQuantity;

    private LocalDateTime updatedAt;
}
