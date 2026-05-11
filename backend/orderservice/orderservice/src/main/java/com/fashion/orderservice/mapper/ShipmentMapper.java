package com.fashion.orderservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.orderservice.dto.common.ShipmentDto;
import com.fashion.orderservice.dto.response.ShipmentResponse;
import com.fashion.orderservice.entity.Shipment;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    ShipmentDto toDto(Shipment entity);

    @Mapping(target = "orderId", source = "order.id")
    ShipmentResponse toResponse(Shipment entity);
}
