package com.fashion.productservice.dto.request.variant;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class CreateVariantRequest {

    private String sku;
    private String color;
    private String size;

    private BigDecimal price;
    private BigDecimal salePrice;
    private BigDecimal weight;

    private String barcode;
}
