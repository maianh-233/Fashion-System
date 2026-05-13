package com.fashion.promotionservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.promotionservice.dto.common.PromotionBrandDto;
import com.fashion.promotionservice.entity.PromotionBrand;

@Mapper(componentModel = "spring")
public interface PromotionBrandMapper {

    @Mapping(target = "brandId", source = "id.brandId")
    PromotionBrandDto toDto(PromotionBrand entity);

    List<PromotionBrandDto> toDtoList(List<PromotionBrand> entities);
}