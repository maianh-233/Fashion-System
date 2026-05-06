package com.fashion.customerservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.fashion.customerservice.dto.request.UpdateProfileRequest;
import com.fashion.customerservice.dto.response.ProfileResponse;
import com.fashion.customerservice.entity.CustomerProfile;


@Mapper(config = MapperCentralConfig.class)
public interface CustomerProfileMapper {

    ProfileResponse toResponse(CustomerProfile entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateProfileRequest request, @MappingTarget CustomerProfile entity);
}
