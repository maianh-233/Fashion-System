package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Manual stock delta; final balances and actor identity are server-owned. */
public record InventoryAdjustmentRequest(
        UUID storeId,
        @NotNull UUID productVariantId,
        @NotNull Integer quantityDelta) {
}
