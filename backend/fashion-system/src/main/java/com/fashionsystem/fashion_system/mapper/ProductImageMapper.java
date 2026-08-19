package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.entity.ProductImage;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ProductImage} và {@link ProductImageDto}.
 */
@Component
public class ProductImageMapper {
    public ProductImageDto toDto(ProductImage entity) {
        if (entity == null) {
            return null;
        }
        ProductImageDto dto = new ProductImageDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ProductImage toEntity(ProductImageDto dto) {
        if (dto == null) {
            return null;
        }
        ProductImage entity = new ProductImage();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

