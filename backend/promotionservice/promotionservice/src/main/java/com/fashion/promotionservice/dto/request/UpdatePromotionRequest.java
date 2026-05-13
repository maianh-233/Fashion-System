package com.fashion.promotionservice.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fashion.promotionservice.dto.common.PromotionConditionDto;
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
public class UpdatePromotionRequest {

    private String name;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscount;

    private Integer usageLimit;

    private Integer usagePerUser;

    private Boolean active;

    private List<UUID> tierIds;

    private List<UUID> productIds;

    private List<UUID> categoryIds;

    private List<UUID> brandIds;

    private List<UUID> collectionIds;

    private List<PromotionConditionDto> conditions;
}