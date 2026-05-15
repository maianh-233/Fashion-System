package com.fashion.chatservice.dto.common;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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
public class InternalChatMessageDto {

    private UUID id;

    private UUID roomId;

    private UUID senderId;

    private String senderType;

    private String content;

    private String intent;

    private Map<String, Object> metadata;

    private LocalDateTime createdAt;
}
