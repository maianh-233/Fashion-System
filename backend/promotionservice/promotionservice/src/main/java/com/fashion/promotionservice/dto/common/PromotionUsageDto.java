package com.fashion.promotionservice.dto.common;

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
public class PromotionUsageDto {

    private UUID id;

    private UUID orderId;

    private UUID userId;

    private LocalDateTime usedAt;
}