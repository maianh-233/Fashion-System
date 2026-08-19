package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.entity.Promotion;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Promotion} và {@link PromotionDto}.
 */
@Component
public class PromotionMapper {
    public PromotionDto toDto(Promotion entity) {
        if (entity == null) {
            return null;
        }
        PromotionDto dto = new PromotionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Promotion toEntity(PromotionDto dto) {
        if (dto == null) {
            return null;
        }
        Promotion entity = new Promotion();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

