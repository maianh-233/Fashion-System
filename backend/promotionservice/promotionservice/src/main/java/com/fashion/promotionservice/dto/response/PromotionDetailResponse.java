package com.fashion.promotionservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fashion.promotionservice.dto.common.PromotionBrandDto;
import com.fashion.promotionservice.dto.common.PromotionCategoryDto;
import com.fashion.promotionservice.dto.common.PromotionCollectionDto;
import com.fashion.promotionservice.dto.common.PromotionConditionDto;
import com.fashion.promotionservice.dto.common.PromotionProductDto;
import com.fashion.promotionservice.dto.common.PromotionTierDto;
import com.fashion.promotionservice.entity.enums.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetailResponse {

    private UUID id;

    private String code;

    private String name;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscount;

    private Integer usageLimit;

    private Integer usagePerUser;

    private Boolean active;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<PromotionConditionDto> conditions;

    private List<PromotionTierDto> tiers;

    private List<PromotionProductDto> products;

    private List<PromotionCategoryDto> categories;

    private List<PromotionBrandDto> brands;

    private List<PromotionCollectionDto> collections;
}