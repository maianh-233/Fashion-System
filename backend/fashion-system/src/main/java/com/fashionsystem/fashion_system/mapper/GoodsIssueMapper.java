package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.entity.GoodsIssue;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link GoodsIssue} và {@link GoodsIssueDto}.
 */
@Component
public class GoodsIssueMapper {
    public GoodsIssueDto toDto(GoodsIssue entity) {
        if (entity == null) {
            return null;
        }
        GoodsIssueDto dto = new GoodsIssueDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public GoodsIssue toEntity(GoodsIssueDto dto) {
        if (dto == null) {
            return null;
        }
        GoodsIssue entity = new GoodsIssue();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

