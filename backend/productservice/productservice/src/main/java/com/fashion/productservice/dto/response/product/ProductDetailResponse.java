package com.fashion.productservice.dto.response.product;

import java.util.List;
import java.util.UUID;

import com.fashion.productservice.dto.response.variant.ProductVariantResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Builder
public class ProductDetailResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;

    private String material;
    private String fit;
    private String gender;
    private String status;

    private String brandName;
    private String collectionName;
    private String categoryName;

    private List<ProductVariantResponse> variants;
}
