package com.fashion.orderservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fashion.orderservice.dto.response.OrderDetailResponse;
import com.fashion.orderservice.dto.response.OrderResponse;
import com.fashion.orderservice.entity.Order;

@Mapper(
        componentModel = "spring",
        uses = {
                OrderItemMapper.class,
                OrderAddressMapper.class,
                ShipmentMapper.class,
                OrderPromotionMapper.class,
                OrderStatusHistoryMapper.class
        }
)
public interface OrderMapper {

    OrderResponse toResponse(Order entity);

    List<OrderResponse> toResponseList(List<Order> entities);

    OrderDetailResponse toDetailResponse(Order entity);
}
