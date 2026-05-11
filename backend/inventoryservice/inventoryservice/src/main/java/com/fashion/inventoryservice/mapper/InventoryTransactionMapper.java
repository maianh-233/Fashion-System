package com.fashion.inventoryservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.inventoryservice.dto.response.InventoryTransactionResponse;
import com.fashion.inventoryservice.entity.InventoryTransaction;

@Mapper(componentModel = "spring")
public interface InventoryTransactionMapper {

    @Mapping(target = "storeId", source = "store.id")
    InventoryTransactionResponse toResponse(InventoryTransaction entity);
}