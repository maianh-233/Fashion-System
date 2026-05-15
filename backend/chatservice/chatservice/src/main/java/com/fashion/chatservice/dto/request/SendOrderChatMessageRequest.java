package com.fashion.chatservice.dto.request;



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
public class SendOrderChatMessageRequest {

    private UUID roomId;

    private UUID senderId;

    private String senderType;

    private String messageType;

    private String content;

    private String relatedAction;

    private Map<String, Object> metadata;
}
