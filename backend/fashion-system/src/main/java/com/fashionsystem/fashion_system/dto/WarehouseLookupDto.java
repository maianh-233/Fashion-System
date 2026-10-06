package com.fashionsystem.fashion_system.dto;

import java.util.UUID;

/** Redis-safe response projections used by warehouse catalog lookups. */
public final class WarehouseLookupDto {
    private WarehouseLookupDto() {
    }

    public record ProductOption(UUID id, String name, String code, UUID categoryId) {}
    public record VariantOption(UUID id, UUID productId, String sku, String color, String size,
            Boolean active) {}
    public record SupplierOption(UUID id, String name, String code) {}
    public record CategoryOption(UUID id, String name) {}
}
