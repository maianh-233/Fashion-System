package com.fashion.inventoryservice.dto.request.inventory;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
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
public class AdjustInventoryRequest {

    @NotNull
    private UUID storeId;

    @NotNull
    private UUID productVariantId;

    @NotNull
    private Integer quantity;

    private String reason;
}