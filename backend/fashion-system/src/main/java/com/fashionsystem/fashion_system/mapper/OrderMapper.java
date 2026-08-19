package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderDto;
import com.fashionsystem.fashion_system.entity.Order;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link Order} và {@link OrderDto}.
 */
@Component
public class OrderMapper {
    public OrderDto toDto(Order entity) {
        if (entity == null) {
            return null;
        }
        OrderDto dto = new OrderDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public Order toEntity(OrderDto dto) {
        if (dto == null) {
            return null;
        }
        Order entity = new Order();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

