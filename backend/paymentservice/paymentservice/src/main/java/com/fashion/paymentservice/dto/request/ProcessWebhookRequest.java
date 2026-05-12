package com.fashion.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class ProcessWebhookRequest {

    @NotBlank
    private String provider;

    @NotBlank
    private String eventType;

    @NotBlank
    private String payload;
}