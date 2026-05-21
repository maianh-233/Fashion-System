package com.fashion.productservice.dto.request.product;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProductRequest {

    private String name;
    private String description;
    private String material;
    private String fit;
    private String gender;
    private String status;
    private String imageUrl;

    private UUID brandId;
    private UUID collectionId;
    private UUID categoryId;
}
