package com.fashion.customerservice.mapper;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.fashion.customerservice.dto.request.CreateAddressRequest;
import com.fashion.customerservice.dto.request.UpdateAddressRequest;
import com.fashion.customerservice.dto.response.AddressResponse;
import com.fashion.customerservice.entity.CustomerAddress;

@Mapper(config = MapperCentralConfig.class)
public interface AddressMapper {

    AddressResponse toResponse(CustomerAddress entity);

    List<AddressResponse> toResponseList(List<CustomerAddress> entities);

    CustomerAddress toEntity(CreateAddressRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateAddressRequest request, @MappingTarget CustomerAddress entity);
}
