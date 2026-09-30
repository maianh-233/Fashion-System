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
        Long outOfStockSku,
        Long offlineQuantity,
        Long onlineQuantity) {
    public Long totalQuantity() { return availableQuantity; }
    @com.fasterxml.jackson.annotation.JsonProperty("totalQuantity")
    public Long getTotalQuantity() { return availableQuantity; }
    public StoreInventoryStatisticsDto(UUID storeId, String storeName, Long totalSku, Long availableQuantity, Long reservedQuantity, Long damagedQuantity, Long lowStockSku, Long outOfStockSku) {
        this(storeId, storeName, totalSku, availableQuantity, reservedQuantity, damagedQuantity, lowStockSku, outOfStockSku, availableQuantity, 0L);
    }
}
