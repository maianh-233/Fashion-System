package com.fashion.promotionservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.promotionservice.dto.common.PromotionCategoryDto;
import com.fashion.promotionservice.entity.PromotionCategory;

@Mapper(componentModel = "spring")
public interface PromotionCategoryMapper {

    @Mapping(target = "categoryId", source = "id.categoryId")
    PromotionCategoryDto toDto(PromotionCategory entity);

    List<PromotionCategoryDto> toDtoList(List<PromotionCategory> entities);
}