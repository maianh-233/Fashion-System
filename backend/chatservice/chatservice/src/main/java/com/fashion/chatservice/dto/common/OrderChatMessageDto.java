package com.fashion.chatservice.dto.common;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderChatMessageDto {

    private UUID id;

    private UUID roomId;

    private UUID senderId;

    private String senderType;

    private String messageType;

    private String content;

    private String relatedAction;

    private Map<String, Object> metadata;

    private List<ChatAttachmentDto> attachments;

    private LocalDateTime createdAt;
}