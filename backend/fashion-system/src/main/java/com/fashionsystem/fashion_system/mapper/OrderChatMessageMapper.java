package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderChatMessageDto;
import com.fashionsystem.fashion_system.entity.OrderChatMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link OrderChatMessage} và {@link OrderChatMessageDto}.
 */
@Component
public class OrderChatMessageMapper {
    public OrderChatMessageDto toDto(OrderChatMessage entity) {
        if (entity == null) {
            return null;
        }
        OrderChatMessageDto dto = new OrderChatMessageDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderChatMessage toEntity(OrderChatMessageDto dto) {
        if (dto == null) {
            return null;
        }
        OrderChatMessage entity = new OrderChatMessage();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

