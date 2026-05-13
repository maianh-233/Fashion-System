package com.fashion.promotionservice.dto.response;

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
public class PromotionApplyResponse {

    private UUID promotionId;

    private String code;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private String message;
}