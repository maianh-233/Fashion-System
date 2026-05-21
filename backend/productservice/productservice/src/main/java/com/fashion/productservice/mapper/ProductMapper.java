package com.fashion.productservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.fashion.productservice.dto.request.product.CreateProductRequest;
import com.fashion.productservice.dto.request.product.UpdateProductRequest;
import com.fashion.productservice.dto.response.product.ProductDetailResponse;
import com.fashion.productservice.dto.response.product.ProductResponse;
import com.fashion.productservice.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // ================= ENTITY -> RESPONSE =================

    @Mapping(source = "brand.name", target = "brandName")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "imageUrl", target = "imageUrl")
    ProductResponse toResponse(Product product);

    @Mapping(source = "brand.name", target = "brandName")
    @Mapping(source = "collection.name", target = "collectionName")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "imageUrl", target = "imageUrl")
    @Mapping(source = "variants", target = "variants")
    ProductDetailResponse toDetailResponse(Product product);

    // ================= REQUEST -> ENTITY =================

    Product toEntity(CreateProductRequest request);

    void updateEntity(@MappingTarget Product product, UpdateProductRequest request);
}
