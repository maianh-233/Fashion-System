package com.fashion.chatservice.dto.response;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fashion.chatservice.dto.common.ChatAttachmentDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderChatMessageResponse {

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
