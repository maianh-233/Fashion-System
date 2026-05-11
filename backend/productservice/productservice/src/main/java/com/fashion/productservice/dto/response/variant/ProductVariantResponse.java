package com.fashion.productservice.dto.response.variant;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Builder
public class ProductVariantResponse {

    private UUID id;
    private String sku;
    private String color;
    private String size;

    private BigDecimal price;
    private BigDecimal salePrice;
    private Boolean active;
}