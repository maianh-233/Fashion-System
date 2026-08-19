package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderPromotionDto;
import com.fashionsystem.fashion_system.entity.OrderPromotion;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link OrderPromotion} và {@link OrderPromotionDto}.
 */
@Component
public class OrderPromotionMapper {
    public OrderPromotionDto toDto(OrderPromotion entity) {
        if (entity == null) {
            return null;
        }
        OrderPromotionDto dto = new OrderPromotionDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderPromotion toEntity(OrderPromotionDto dto) {
        if (dto == null) {
            return null;
        }
        OrderPromotion entity = new OrderPromotion();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

