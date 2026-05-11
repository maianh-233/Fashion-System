package com.fashion.orderservice.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.fashion.orderservice.dto.common.OrderPromotionDto;
import com.fashion.orderservice.entity.OrderPromotion;

@Mapper(componentModel = "spring")
public interface OrderPromotionMapper {

    OrderPromotionDto toDto(OrderPromotion entity);

    List<OrderPromotionDto> toDtoList(List<OrderPromotion> entities);
}
