package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionUsageDto;
import com.fashionsystem.fashion_system.entity.PromotionUsage;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PromotionUsage} và {@link PromotionUsageDto}.
 */
@Component
public class PromotionUsageMapper {
    public PromotionUsageDto toDto(PromotionUsage entity) {
        if (entity == null) {
            return null;
        }
        PromotionUsageDto dto = new PromotionUsageDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PromotionUsage toEntity(PromotionUsageDto dto) {
        if (dto == null) {
            return null;
        }
        PromotionUsage entity = new PromotionUsage();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

