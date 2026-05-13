package com.fashion.promotionservice.dto.response;

import java.time.LocalDateTime;
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
public class PromotionUsageResponse {

    private UUID id;

    private UUID promotionId;

    private UUID orderId;

    private UUID userId;

    private LocalDateTime usedAt;
}