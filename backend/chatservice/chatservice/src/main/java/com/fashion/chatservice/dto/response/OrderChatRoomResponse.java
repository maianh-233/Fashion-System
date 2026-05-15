package com.fashion.chatservice.dto.response;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fashion.chatservice.dto.common.OrderChatMessageDto;

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
public class OrderChatRoomResponse {

    private UUID id;

    private UUID orderId;

    private UUID customerId;

    private UUID assignedStaffId;

    private String status;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    private List<OrderChatMessageDto> messages;
}