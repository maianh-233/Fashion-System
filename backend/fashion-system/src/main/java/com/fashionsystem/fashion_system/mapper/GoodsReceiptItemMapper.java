package com.fashionsystem.fashion_system.mapper;
import com.fashionsystem.fashion_system.dto.GoodsReceiptItemDto;
import com.fashionsystem.fashion_system.entity.GoodsReceiptItem;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
@Component
public class GoodsReceiptItemMapper {
    public GoodsReceiptItemDto toDto(GoodsReceiptItem entity) {
        if (entity == null) return null;
        var dto = new GoodsReceiptItemDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
