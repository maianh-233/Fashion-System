package com.fashion.inventoryservice.dto.request.goodsreceipt;

import java.math.BigDecimal;
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
public class CreateGoodsReceiptItemRequest {

    @NotNull
    private UUID productVariantId;

    private String sku;

    private String productName;

    @NotNull
    private BigDecimal costPrice;

    @NotNull
    private Integer quantity;
}
