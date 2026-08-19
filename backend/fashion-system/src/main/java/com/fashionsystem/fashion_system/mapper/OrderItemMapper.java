package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderItemDto;
import com.fashionsystem.fashion_system.entity.OrderItem;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/** Mapper chuyển đổi hai chiều giữa {@link OrderItem} và {@link OrderItemDto}. */
@Component
public class OrderItemMapper {
    public OrderItemDto toDto(OrderItem entity) {
        if (entity == null) return null;
        OrderItemDto dto = new OrderItemDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
    public OrderItem toEntity(OrderItemDto dto) {
        if (dto == null) return null;
        OrderItem entity = new OrderItem();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}
