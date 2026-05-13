package com.fashion.promotionservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.promotionservice.dto.common.PromotionUsageDto;
import com.fashion.promotionservice.dto.response.PromotionUsageResponse;
import com.fashion.promotionservice.entity.PromotionUsage;

@Mapper(componentModel = "spring")
public interface PromotionUsageMapper {

    @Mapping(target = "promotionId", source = "promotion.id")
    PromotionUsageResponse toResponse(PromotionUsage entity);

    PromotionUsageDto toDto(PromotionUsage entity);

    List<PromotionUsageResponse> toResponseList(List<PromotionUsage> entities);
}