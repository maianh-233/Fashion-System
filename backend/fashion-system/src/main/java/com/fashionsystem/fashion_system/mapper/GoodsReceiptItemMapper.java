package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.GoodsReceiptItemDto;
import com.fashionsystem.fashion_system.entity.GoodsReceiptItem;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link GoodsReceiptItem} và {@link GoodsReceiptItemDto}.
 */
@Component
public class GoodsReceiptItemMapper {
    public GoodsReceiptItemDto toDto(GoodsReceiptItem entity) {
        if (entity == null) {
            return null;
        }
        GoodsReceiptItemDto dto = new GoodsReceiptItemDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public GoodsReceiptItem toEntity(GoodsReceiptItemDto dto) {
        if (dto == null) {
            return null;
        }
        GoodsReceiptItem entity = new GoodsReceiptItem();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

