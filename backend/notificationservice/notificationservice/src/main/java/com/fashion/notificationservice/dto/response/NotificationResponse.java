package com.fashion.notificationservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.notificationservice.entity.enums.NotificationChannel;
import com.fashion.notificationservice.entity.enums.NotificationRecipientType;
import com.fashion.notificationservice.entity.enums.NotificationStatus;
import com.fashion.notificationservice.entity.enums.ReferenceType;

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
public class NotificationResponse {

    private UUID id;

    private UUID recipientId;

    private NotificationRecipientType recipientType;

    private NotificationChannel channel;

    private String title;

    private String content;

    private ReferenceType referenceType;

    private UUID referenceId;

    private NotificationStatus status;

    private LocalDateTime scheduledAt;

    private LocalDateTime createdAt;

    private LocalDateTime sentAt;
}