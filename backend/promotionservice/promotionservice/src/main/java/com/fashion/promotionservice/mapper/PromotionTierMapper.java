
package com.fashion.promotionservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.promotionservice.dto.common.PromotionTierDto;
import com.fashion.promotionservice.entity.PromotionTier;

@Mapper(componentModel = "spring")
public interface PromotionTierMapper {

    @Mapping(target = "tierId", source = "id.tierId")
    PromotionTierDto toDto(PromotionTier entity);

    List<PromotionTierDto> toDtoList(List<PromotionTier> entities);
}