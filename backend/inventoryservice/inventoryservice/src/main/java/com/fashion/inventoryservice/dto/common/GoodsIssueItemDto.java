package com.fashion.inventoryservice.dto.common;

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
public class GoodsIssueItemDto {

    private UUID productVariantId;

    private String sku;

    private String productName;

    private Integer quantity;
}
