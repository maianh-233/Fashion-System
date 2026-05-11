package com.fashion.inventoryservice.mapper;

import com.fashion.inventoryservice.dto.request.store.CreateStoreRequest;
import com.fashion.inventoryservice.dto.request.store.UpdateStoreRequest;
import com.fashion.inventoryservice.dto.response.StoreResponse;
import com.fashion.inventoryservice.entity.Store;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    Store toEntity(CreateStoreRequest request);

    StoreResponse toResponse(Store entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateStore(
            UpdateStoreRequest request,
            @MappingTarget Store entity
    );
}