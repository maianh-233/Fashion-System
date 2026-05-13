package com.fashion.notificationservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.notificationservice.entity.enums.NotificationChannel;
import com.fashion.notificationservice.entity.enums.NotificationStatus;

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
public class NotificationLogResponse {

    private UUID id;

    private UUID notificationId;

    private NotificationChannel channel;

    private String destination;

    private NotificationStatus status;

    private String errorMessage;

    private String provider;

    private LocalDateTime sentAt;
}