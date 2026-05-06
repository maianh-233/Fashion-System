package com.fashion.customerservice.mapper;

import org.mapstruct.Mapper;

import com.fashion.customerservice.dto.response.LoyaltyResponse;
import com.fashion.customerservice.entity.LoyaltyAccount;

@Mapper(config = MapperCentralConfig.class)
public interface LoyaltyMapper {

    LoyaltyResponse toResponse(LoyaltyAccount entity);
}