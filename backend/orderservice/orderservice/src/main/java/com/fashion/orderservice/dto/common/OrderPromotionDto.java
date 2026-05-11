package com.fashion.orderservice.dto.common;


import java.math.BigDecimal;
import java.util.UUID;

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
public class OrderPromotionDto {

    private UUID promotionId;

    private String promotionCode;

    private BigDecimal discountAmount;
}