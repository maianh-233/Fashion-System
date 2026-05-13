package com.fashion.notificationservice.dto.response;

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
public class NotificationStatisticsResponse {

    private Long totalNotifications;

    private Long sentNotifications;

    private Long failedNotifications;

    private Long pendingNotifications;

    private Long cancelledNotifications;
}