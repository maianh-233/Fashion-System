package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của NotificationLog giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogDto {
    private UUID id;
    private UUID notificationId;
    private String channel;
    private String destination;
    private String status;
    private String errorMessage;
    private String provider;
    private LocalDateTime sentAt;
}

