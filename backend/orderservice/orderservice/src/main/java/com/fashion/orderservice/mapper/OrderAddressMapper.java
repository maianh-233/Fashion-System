package com.fashion.orderservice.mapper;

import org.mapstruct.Mapper;

import com.fashion.orderservice.dto.common.OrderAddressDto;
import com.fashion.orderservice.entity.OrderAddress;

@Mapper(componentModel = "spring")
public interface OrderAddressMapper {

    OrderAddressDto toDto(OrderAddress entity);

    OrderAddress toEntity(OrderAddressDto dto);
}