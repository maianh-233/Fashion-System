package com.fashion.inventoryservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.inventoryservice.dto.response.StockReservationResponse;
import com.fashion.inventoryservice.entity.StockReservation;

@Mapper(componentModel = "spring")
public interface StockReservationMapper {

    @Mapping(target = "storeId", source = "store.id")
    StockReservationResponse toResponse(StockReservation entity);
}