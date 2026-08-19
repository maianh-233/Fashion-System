package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.entity.GoodsReceipt;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link GoodsReceipt} và {@link GoodsReceiptDto}.
 */
@Component
public class GoodsReceiptMapper {
    public GoodsReceiptDto toDto(GoodsReceipt entity) {
        if (entity == null) {
            return null;
        }
        GoodsReceiptDto dto = new GoodsReceiptDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public GoodsReceipt toEntity(GoodsReceiptDto dto) {
        if (dto == null) {
            return null;
        }
        GoodsReceipt entity = new GoodsReceipt();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

