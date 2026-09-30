package com.fashionsystem.fashion_system.mapper;
import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.entity.GoodsReceipt;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
@Component
public class GoodsReceiptMapper {
    public GoodsReceiptDto toDto(GoodsReceipt entity) {
        if (entity == null) return null;
        var dto = new GoodsReceiptDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
