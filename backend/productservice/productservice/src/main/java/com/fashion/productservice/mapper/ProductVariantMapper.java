package com.fashion.productservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fashion.productservice.dto.request.variant.CreateVariantRequest;
import com.fashion.productservice.dto.response.variant.ProductVariantResponse;
import com.fashion.productservice.entity.ProductVariant;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {

    ProductVariantResponse toResponse(ProductVariant variant);

    List<ProductVariantResponse> toResponseList(List<ProductVariant> variants);

    ProductVariant toEntity(CreateVariantRequest request);
}
