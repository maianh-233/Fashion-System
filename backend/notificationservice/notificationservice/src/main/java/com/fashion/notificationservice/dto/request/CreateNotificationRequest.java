package com.fashion.notificationservice.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.notificationservice.entity.enums.NotificationChannel;
import com.fashion.notificationservice.entity.enums.NotificationRecipientType;
import com.fashion.notificationservice.entity.enums.ReferenceType;

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
public class CreateNotificationRequest {

    private UUID recipientId;

    @NotNull(message = "Recipient type is required")
    private NotificationRecipientType recipientType;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    private ReferenceType referenceType;

    private UUID referenceId;

    private LocalDateTime scheduledAt;
}