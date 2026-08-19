package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionTierDto;
import com.fashionsystem.fashion_system.entity.PromotionTier;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PromotionTier} và {@link PromotionTierDto}.
 */
@Component
public class PromotionTierMapper {
    public PromotionTierDto toDto(PromotionTier entity) {
        if (entity == null) {
            return null;
        }
        PromotionTierDto dto = new PromotionTierDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PromotionTier toEntity(PromotionTierDto dto) {
        if (dto == null) {
            return null;
        }
        PromotionTier entity = new PromotionTier();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

