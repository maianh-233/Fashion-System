package com.fashion.promotionservice.dto.request;

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
public class ValidatePromotionRequest {

    private String code;

    private UUID userId;

    private BigDecimal orderTotal;
}