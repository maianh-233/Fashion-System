package com.fashion.customerservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.customerservice.dto.response.UserTierResponse;
import com.fashion.customerservice.entity.UserTier;

@Mapper(config = MapperCentralConfig.class)
public interface TierMapper {

    @Mapping(source = "tier.code", target = "code")
    @Mapping(source = "tier.name", target = "name")
    @Mapping(source = "tier.discountPercent", target = "discountPercent")
    UserTierResponse toResponse(UserTier entity);
}
