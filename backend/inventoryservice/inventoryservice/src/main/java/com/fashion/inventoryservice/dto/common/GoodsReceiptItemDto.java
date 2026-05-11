package com.fashion.inventoryservice.dto.common;

import java.math.BigDecimal;
import java.util.UUID;

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
public class GoodsReceiptItemDto {

    private UUID productVariantId;

    private String sku;

    private String productName;

    private BigDecimal costPrice;

    private Integer quantity;

    private BigDecimal total;
}
