package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.ProductTagMappingDto;
import com.fashionsystem.fashion_system.entity.ProductTagMapping;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link ProductTagMapping} và {@link ProductTagMappingDto}.
 */
@Component
public class ProductTagMappingMapper {
    public ProductTagMappingDto toDto(ProductTagMapping entity) {
        if (entity == null) {
            return null;
        }
        ProductTagMappingDto dto = new ProductTagMappingDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public ProductTagMapping toEntity(ProductTagMappingDto dto) {
        if (dto == null) {
            return null;
        }
        ProductTagMapping entity = new ProductTagMapping();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

