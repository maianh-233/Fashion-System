package com.fashion.notificationservice.dto.request;

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
public class NotificationSearchRequest {

    private UUID recipientId;

    private NotificationChannel channel;

    private NotificationStatus status;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 10;
}