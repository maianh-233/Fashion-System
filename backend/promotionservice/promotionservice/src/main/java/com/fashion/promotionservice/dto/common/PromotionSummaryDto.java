package com.fashion.promotionservice.dto.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
public class PromotionSummaryDto {

    private UUID id;

    private String code;

    private String name;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private Boolean active;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}