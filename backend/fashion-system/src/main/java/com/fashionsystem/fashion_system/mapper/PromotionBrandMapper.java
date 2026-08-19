package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionBrandDto;
import com.fashionsystem.fashion_system.entity.PromotionBrand;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PromotionBrand} và {@link PromotionBrandDto}.
 */
@Component
public class PromotionBrandMapper {
    public PromotionBrandDto toDto(PromotionBrand entity) {
        if (entity == null) {
            return null;
        }
        PromotionBrandDto dto = new PromotionBrandDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PromotionBrand toEntity(PromotionBrandDto dto) {
        if (dto == null) {
            return null;
        }
        PromotionBrand entity = new PromotionBrand();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

