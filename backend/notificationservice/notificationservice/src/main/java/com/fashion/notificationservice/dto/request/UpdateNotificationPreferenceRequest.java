package com.fashion.notificationservice.dto.request;

import com.fashion.notificationservice.entity.enums.NotificationChannel;

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
public class UpdateNotificationPreferenceRequest {

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotNull(message = "Enabled is required")
    private Boolean enabled;
}