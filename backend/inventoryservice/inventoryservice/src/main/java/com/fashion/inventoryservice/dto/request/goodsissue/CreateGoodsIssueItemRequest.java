package com.fashion.inventoryservice.dto.request.goodsissue;

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
public class CreateGoodsIssueItemRequest {

    @NotNull
    private UUID productVariantId;

    private String sku;

    private String productName;

    @NotNull
    private Integer quantity;
}
