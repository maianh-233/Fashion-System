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
public class SendInternalChatMessageRequest {

    private UUID roomId;

    private UUID senderId;

    private String senderType;

    private String content;

    private String intent;

    private Map<String, Object> metadata;
}
