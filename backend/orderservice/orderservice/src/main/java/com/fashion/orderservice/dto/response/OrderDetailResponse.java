package com.fashion.orderservice.dto.response;

import java.util.List;

import com.fashion.orderservice.dto.common.OrderAddressDto;
import com.fashion.orderservice.dto.common.OrderItemDto;
import com.fashion.orderservice.dto.common.OrderPromotionDto;
import com.fashion.orderservice.dto.common.OrderStatusHistoryDto;
import com.fashion.orderservice.dto.common.ShipmentDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse extends OrderResponse {

    private List<OrderItemDto> items;

    private OrderAddressDto address;

    private ShipmentDto shipment;

    private List<OrderPromotionDto> promotions;

    private List<OrderStatusHistoryDto> statusHistories;

    private String note;
}
