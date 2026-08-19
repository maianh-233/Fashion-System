package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderIssueTypeDto;
import com.fashionsystem.fashion_system.entity.OrderIssueType;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link OrderIssueType} và {@link OrderIssueTypeDto}.
 */
@Component
public class OrderIssueTypeMapper {
    public OrderIssueTypeDto toDto(OrderIssueType entity) {
        if (entity == null) {
            return null;
        }
        OrderIssueTypeDto dto = new OrderIssueTypeDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderIssueType toEntity(OrderIssueTypeDto dto) {
        if (dto == null) {
            return null;
        }
        OrderIssueType entity = new OrderIssueType();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

