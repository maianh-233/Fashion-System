package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ProductVariant} và {@link ProductVariantDto}.
 */
@Component
public class ProductVariantMapper {
    public ProductVariantDto toDto(ProductVariant entity) {
        if (entity == null) {
            return null;
        }
        ProductVariantDto dto = new ProductVariantDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ProductVariant toEntity(ProductVariantDto dto) {
        if (dto == null) {
            return null;
        }
        ProductVariant entity = new ProductVariant();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

