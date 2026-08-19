package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của NotificationTemplate giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateDto {
    private UUID id;
    private String code;
    private String titleTemplate;
    private String contentTemplate;
    private String channel;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

