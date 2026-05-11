package com.fashion.orderservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fashion.orderservice.dto.common.OrderStatusHistoryDto;
import com.fashion.orderservice.dto.response.OrderStatusHistoryResponse;
import com.fashion.orderservice.entity.OrderStatusHistory;

@Mapper(componentModel = "spring")
public interface OrderStatusHistoryMapper {

    OrderStatusHistoryDto toDto(OrderStatusHistory entity);

    List<OrderStatusHistoryDto> toDtoList(List<OrderStatusHistory> entities);

    OrderStatusHistoryResponse toResponse(OrderStatusHistory entity);

    List<OrderStatusHistoryResponse> toResponseList(List<OrderStatusHistory> entities);
}
