package com.fashion.paymentservice.dto.common;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookLogDto {

    private UUID id;

    private String provider;

    private String eventType;

    private String payload;

    private Boolean processed;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;
}