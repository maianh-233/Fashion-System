package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionCategoryDto;
import com.fashionsystem.fashion_system.entity.PromotionCategory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PromotionCategory} và {@link PromotionCategoryDto}.
 */
@Component
public class PromotionCategoryMapper {
    public PromotionCategoryDto toDto(PromotionCategory entity) {
        if (entity == null) {
            return null;
        }
        PromotionCategoryDto dto = new PromotionCategoryDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PromotionCategory toEntity(PromotionCategoryDto dto) {
        if (dto == null) {
            return null;
        }
        PromotionCategory entity = new PromotionCategory();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

