package com.fashion.promotionservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.promotionservice.dto.common.PromotionCollectionDto;
import com.fashion.promotionservice.entity.PromotionCollection;

@Mapper(componentModel = "spring")
public interface PromotionCollectionMapper {

    @Mapping(target = "collectionId", source = "id.collectionId")
    PromotionCollectionDto toDto(PromotionCollection entity);

    List<PromotionCollectionDto> toDtoList(List<PromotionCollection> entities);
}