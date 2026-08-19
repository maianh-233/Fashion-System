package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.GoodsIssueItemDto;
import com.fashionsystem.fashion_system.entity.GoodsIssueItem;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link GoodsIssueItem} và {@link GoodsIssueItemDto}.
 */
@Component
public class GoodsIssueItemMapper {
    public GoodsIssueItemDto toDto(GoodsIssueItem entity) {
        if (entity == null) {
            return null;
        }
        GoodsIssueItemDto dto = new GoodsIssueItemDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public GoodsIssueItem toEntity(GoodsIssueItemDto dto) {
        if (dto == null) {
            return null;
        }
        GoodsIssueItem entity = new GoodsIssueItem();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

