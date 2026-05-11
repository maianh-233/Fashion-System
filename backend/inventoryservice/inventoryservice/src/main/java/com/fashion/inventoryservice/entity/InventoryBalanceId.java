package com.fashion.inventoryservice.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InventoryBalanceId implements Serializable {

    private UUID store;
    private UUID productVariantId;
}
