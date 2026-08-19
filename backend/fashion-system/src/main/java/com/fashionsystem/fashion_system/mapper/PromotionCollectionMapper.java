package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionCollectionDto;
import com.fashionsystem.fashion_system.entity.PromotionCollection;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PromotionCollection} và {@link PromotionCollectionDto}.
 */
@Component
public class PromotionCollectionMapper {
    public PromotionCollectionDto toDto(PromotionCollection entity) {
        if (entity == null) {
            return null;
        }
        PromotionCollectionDto dto = new PromotionCollectionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PromotionCollection toEntity(PromotionCollectionDto dto) {
        if (dto == null) {
            return null;
        }
        PromotionCollection entity = new PromotionCollection();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

