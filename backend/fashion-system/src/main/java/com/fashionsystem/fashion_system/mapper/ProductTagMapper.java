package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductTagDto;
import com.fashionsystem.fashion_system.entity.ProductTag;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ProductTag} và {@link ProductTagDto}.
 */
@Component
public class ProductTagMapper {
    public ProductTagDto toDto(ProductTag entity) {
        if (entity == null) {
            return null;
        }
        ProductTagDto dto = new ProductTagDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ProductTag toEntity(ProductTagDto dto) {
        if (dto == null) {
            return null;
        }
        ProductTag entity = new ProductTag();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

