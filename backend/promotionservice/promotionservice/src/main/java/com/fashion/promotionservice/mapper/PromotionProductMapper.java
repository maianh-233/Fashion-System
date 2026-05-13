
package com.fashion.promotionservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fashion.promotionservice.dto.common.PromotionProductDto;
import com.fashion.promotionservice.entity.PromotionProduct;

@Mapper(componentModel = "spring")
public interface PromotionProductMapper {

    @Mapping(target = "productId", source = "id.productId")
    PromotionProductDto toDto(PromotionProduct entity);

    List<PromotionProductDto> toDtoList(List<PromotionProduct> entities);
}