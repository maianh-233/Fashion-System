package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderStatusHistoryDto;
import com.fashionsystem.fashion_system.entity.OrderStatusHistory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link OrderStatusHistory} và {@link OrderStatusHistoryDto}.
 */
@Component
public class OrderStatusHistoryMapper {
    public OrderStatusHistoryDto toDto(OrderStatusHistory entity) {
        if (entity == null) {
            return null;
        }
        OrderStatusHistoryDto dto = new OrderStatusHistoryDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderStatusHistory toEntity(OrderStatusHistoryDto dto) {
        if (dto == null) {
            return null;
        }
        OrderStatusHistory entity = new OrderStatusHistory();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

