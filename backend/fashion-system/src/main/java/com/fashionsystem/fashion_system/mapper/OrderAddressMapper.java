package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.OrderAddressDto;
import com.fashionsystem.fashion_system.entity.OrderAddress;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi hai chiều giữa {@link OrderAddress} và {@link OrderAddressDto}.
 */
@Component
public class OrderAddressMapper {
    public OrderAddressDto toDto(OrderAddress entity) {
        if (entity == null) {
            return null;
        }
        OrderAddressDto dto = new OrderAddressDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    public OrderAddress toEntity(OrderAddressDto dto) {
        if (dto == null) {
            return null;
        }
        OrderAddress entity = new OrderAddress();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}

