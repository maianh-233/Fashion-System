package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.entity.Promotion;
import java.time.LocalDateTime;
import java.util.Locale;
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
        entity.setCreatedAt(LocalDateTime.now());
        updateEntity(dto, entity);
        return entity;
    }

    public void updateEntity(PromotionDto dto, Promotion entity) {
        entity.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        entity.setName(dto.getName().trim());
        entity.setDiscountType(dto.getDiscountType().trim().toUpperCase(Locale.ROOT));
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setMinOrderValue(dto.getMinOrderValue());
        entity.setMaxDiscount(dto.getMaxDiscount());
        entity.setUsageLimit(dto.getUsageLimit());
        entity.setUsagePerUser(dto.getUsagePerUser());
        entity.setActive(dto.getActive() == null ? Boolean.TRUE : dto.getActive());
        if (entity.getId() != null) entity.setUpdatedAt(LocalDateTime.now());
    }
}
