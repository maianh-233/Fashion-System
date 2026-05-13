package com.fashion.promotionservice.dto.request;

import java.math.BigDecimal;
import java.util.List;
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
public class ApplyPromotionRequest {

    private String code;

    private UUID userId;

    private UUID orderId;

    private BigDecimal orderTotal;

    private List<UUID> productIds;

    private List<UUID> categoryIds;

    private List<UUID> brandIds;

    private UUID tierId;
}