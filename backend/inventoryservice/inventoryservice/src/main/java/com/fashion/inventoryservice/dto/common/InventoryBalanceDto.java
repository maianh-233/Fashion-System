package com.fashion.inventoryservice.dto.common;

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
public class InventoryBalanceDto {

    private Integer availableQuantity;

    private Integer reservedQuantity;

    private Integer damagedQuantity;
}