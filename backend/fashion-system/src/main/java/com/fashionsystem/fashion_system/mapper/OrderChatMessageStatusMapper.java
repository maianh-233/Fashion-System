package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderChatMessageStatusDto;
import com.fashionsystem.fashion_system.entity.OrderChatMessageStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link OrderChatMessageStatus} và {@link OrderChatMessageStatusDto}.
 */
@Component
public class OrderChatMessageStatusMapper {
    public OrderChatMessageStatusDto toDto(OrderChatMessageStatus entity) {
        if (entity == null) {
            return null;
        }
        OrderChatMessageStatusDto dto = new OrderChatMessageStatusDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderChatMessageStatus toEntity(OrderChatMessageStatusDto dto) {
        if (dto == null) {
            return null;
        }
        OrderChatMessageStatus entity = new OrderChatMessageStatus();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

