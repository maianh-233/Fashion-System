package com.fashion.productservice.dto.response.product;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String slug;
    private String status;
    private String imageUrl;
    private String brandName;
    private String categoryName;
    private String categoryImageUrl;
}
