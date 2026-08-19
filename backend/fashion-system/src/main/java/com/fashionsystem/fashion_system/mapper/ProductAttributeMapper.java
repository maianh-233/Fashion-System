package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductAttributeDto;
import com.fashionsystem.fashion_system.entity.ProductAttribute;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ProductAttribute} và {@link ProductAttributeDto}.
 */
@Component
public class ProductAttributeMapper {
    public ProductAttributeDto toDto(ProductAttribute entity) {
        if (entity == null) {
            return null;
        }
        ProductAttributeDto dto = new ProductAttributeDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ProductAttribute toEntity(ProductAttributeDto dto) {
        if (dto == null) {
            return null;
        }
        ProductAttribute entity = new ProductAttribute();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

