package com.fashion.promotionservice.mapper;

import com.fashion.promotionservice.dto.request.CreatePromotionRequest;
import com.fashion.promotionservice.dto.request.UpdatePromotionRequest;
import com.fashion.promotionservice.dto.response.PromotionDetailResponse;
import com.fashion.promotionservice.dto.response.PromotionResponse;
import com.fashion.promotionservice.entity.Promotion;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                PromotionConditionMapper.class,
                PromotionTierMapper.class,
                PromotionProductMapper.class,
                PromotionCategoryMapper.class,
                PromotionBrandMapper.class,
                PromotionCollectionMapper.class
        }
)
public interface PromotionMapper {

    PromotionResponse toResponse(Promotion entity);

    List<PromotionResponse> toResponseList(List<Promotion> entities);

    @Mappings({
            @Mapping(target = "conditions", source = "conditions"),
            @Mapping(target = "tiers", ignore = true),
            @Mapping(target = "products", ignore = true),
            @Mapping(target = "categories", ignore = true),
            @Mapping(target = "brands", ignore = true),
            @Mapping(target = "collections", ignore = true)
    })
    PromotionDetailResponse toDetailResponse(Promotion entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(
            UpdatePromotionRequest request,
            @MappingTarget Promotion entity
    );

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "usages", ignore = true),
            @Mapping(target = "conditions", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    Promotion toEntity(CreatePromotionRequest request);
}