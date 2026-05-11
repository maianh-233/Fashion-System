package com.fashion.inventoryservice.mapper;

import com.fashion.inventoryservice.dto.request.supplier.CreateSupplierRequest;
import com.fashion.inventoryservice.dto.request.supplier.UpdateSupplierRequest;
import com.fashion.inventoryservice.dto.response.SupplierResponse;
import com.fashion.inventoryservice.entity.Supplier;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    Supplier toEntity(CreateSupplierRequest request);

    SupplierResponse toResponse(Supplier entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSupplier(
            UpdateSupplierRequest request,
            @MappingTarget Supplier entity
    );
}
