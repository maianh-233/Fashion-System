package com.fashion.chatservice.dto.response;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fashion.chatservice.dto.common.InternalChatMessageDto;

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
public class InternalChatRoomResponse {

    private UUID id;

    private UUID createdBy;

    private String roomType;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    private List<InternalChatMessageDto> messages;
}