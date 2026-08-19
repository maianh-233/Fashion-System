package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của OrderChatMessage giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderChatMessageDto {
    private UUID id;
    private UUID roomId;
    private UUID senderId;
    private String senderType;
    private String messageType;
    private String content;
    private String relatedAction;
    private String metadata;
    private LocalDateTime createdAt;
}

