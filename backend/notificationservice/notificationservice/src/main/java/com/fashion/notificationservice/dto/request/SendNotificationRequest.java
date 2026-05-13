package com.fashion.notificationservice.dto.request;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SendNotificationRequest {

    @NotNull(message = "Recipient ID is required")
    private UUID recipientId;

    @NotBlank(message = "Template code is required")
    private String templateCode;

    private Map<String, Object> variables;
}