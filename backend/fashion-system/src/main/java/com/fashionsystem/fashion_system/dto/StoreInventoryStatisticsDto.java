package com.fashionsystem.fashion_system.dto;

import java.util.UUID;

/** Database aggregate for one Store or a requested overall scope. */
public record StoreInventoryStatisticsDto(
        UUID storeId,
        String storeName,
        Long totalSku,
        Long availableQuantity,
        Long reservedQuantity,
        Long damagedQuantity,
        Long lowStockSku,
        Long outOfStockSku) {
}
