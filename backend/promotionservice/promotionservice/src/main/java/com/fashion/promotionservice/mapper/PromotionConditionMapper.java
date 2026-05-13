package com.fashion.promotionservice.mapper;

import com.fashion.promotionservice.dto.common.PromotionConditionDto;
import com.fashion.promotionservice.entity.PromotionCondition;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PromotionConditionMapper {

    PromotionConditionDto toDto(PromotionCondition entity);

    PromotionCondition toEntity(PromotionConditionDto dto);

    List<PromotionConditionDto> toDtoList(List<PromotionCondition> entities);
}