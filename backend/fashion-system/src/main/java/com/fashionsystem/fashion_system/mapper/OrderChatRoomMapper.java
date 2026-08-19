package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderChatRoomDto;
import com.fashionsystem.fashion_system.entity.OrderChatRoom;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link OrderChatRoom} và {@link OrderChatRoomDto}.
 */
@Component
public class OrderChatRoomMapper {
    public OrderChatRoomDto toDto(OrderChatRoom entity) {
        if (entity == null) {
            return null;
        }
        OrderChatRoomDto dto = new OrderChatRoomDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderChatRoom toEntity(OrderChatRoomDto dto) {
        if (dto == null) {
            return null;
        }
        OrderChatRoom entity = new OrderChatRoom();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

