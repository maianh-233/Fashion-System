package com.fashion.inventoryservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.inventoryservice.dto.response.InventoryBalanceResponse;
import com.fashion.inventoryservice.entity.InventoryBalance;

@Mapper(componentModel = "spring")
public interface InventoryBalanceMapper {

    @Mapping(target = "storeId", source = "store.id")
    InventoryBalanceResponse toResponse(InventoryBalance entity);
}