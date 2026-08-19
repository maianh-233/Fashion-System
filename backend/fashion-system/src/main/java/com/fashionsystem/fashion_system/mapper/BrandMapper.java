package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.entity.Brand;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Brand} và {@link BrandDto}.
 */
@Component
public class BrandMapper {
    public BrandDto toDto(Brand entity) {
        if (entity == null) {
            return null;
        }
        BrandDto dto = new BrandDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Brand toEntity(BrandDto dto) {
        if (dto == null) {
            return null;
        }
        Brand entity = new Brand();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

