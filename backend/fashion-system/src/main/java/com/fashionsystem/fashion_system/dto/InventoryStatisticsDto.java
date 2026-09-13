package com.fashionsystem.fashion_system.dto;

import java.util.List;
import java.util.UUID;

/** Scoped Inventory dashboard response with optional Global per-Store breakdown. */
public record InventoryStatisticsDto(
        String scope,
        UUID storeId,
        String storeName,
        StoreInventoryStatisticsDto totals,
        List<StoreInventoryStatisticsDto> byStore) {
}
