package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.entity.Promotion;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển {@link Promotion} sang {@link PromotionDto}. Ghi dữ liệu vào entity do PromotionService đảm nhận
 * để giữ các quy tắc theo loại khuyến mãi ở một chỗ.
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
}
