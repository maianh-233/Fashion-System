package com.fashion.orderservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fashion.orderservice.dto.common.OrderItemDto;
import com.fashion.orderservice.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    OrderItemDto toDto(OrderItem entity);

    List<OrderItemDto> toDtoList(List<OrderItem> entities);
}