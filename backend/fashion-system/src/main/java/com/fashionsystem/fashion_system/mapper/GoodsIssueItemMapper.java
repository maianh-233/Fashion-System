package com.fashionsystem.fashion_system.mapper;
import com.fashionsystem.fashion_system.dto.GoodsIssueItemDto;
import com.fashionsystem.fashion_system.entity.GoodsIssueItem;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
@Component
public class GoodsIssueItemMapper {
    public GoodsIssueItemDto toDto(GoodsIssueItem entity) {
        if (entity == null) return null;
        var dto = new GoodsIssueItemDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
