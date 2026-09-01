package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionConditionDto;
import com.fashionsystem.fashion_system.entity.PromotionCondition;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PromotionCondition} và {@link PromotionConditionDto}.
 */
@Component
public class PromotionConditionMapper {
    public PromotionConditionDto toDto(PromotionCondition entity) {
        if (entity == null) {
            return null;
        }
        PromotionConditionDto dto = new PromotionConditionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PromotionCondition toEntity(PromotionConditionDto dto) {
        if (dto == null) {
            return null;
        }
        PromotionCondition entity = new PromotionCondition();
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(PromotionConditionDto dto, PromotionCondition entity) {
        entity.setConditionType(dto.getConditionType().trim().toUpperCase(Locale.ROOT));
        entity.setConditionValue(dto.getConditionValue().trim());
    }
}
