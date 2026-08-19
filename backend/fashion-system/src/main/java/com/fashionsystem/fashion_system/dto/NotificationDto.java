package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của Notification giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID id;
    private UUID recipientId;
    private String recipientType;
    private String channel;
    private String title;
    private String content;
    private String referenceType;
    private UUID referenceId;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}

