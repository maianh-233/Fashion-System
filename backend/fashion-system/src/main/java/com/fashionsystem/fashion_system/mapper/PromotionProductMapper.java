package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionProductDto;
import com.fashionsystem.fashion_system.entity.PromotionProduct;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link PromotionProduct} và {@link PromotionProductDto}.
 */
@Component
public class PromotionProductMapper {
    public PromotionProductDto toDto(PromotionProduct entity) {
        if (entity == null) {
            return null;
        }
        PromotionProductDto dto = new PromotionProductDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public PromotionProduct toEntity(PromotionProductDto dto) {
        if (dto == null) {
            return null;
        }
        PromotionProduct entity = new PromotionProduct();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

