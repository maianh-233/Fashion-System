package com.fashion.notificationservice.dto.common;

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
public class NotificationFilter {

    private UUID recipientId;

    private NotificationChannel channel;

    private NotificationStatus status;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;
}