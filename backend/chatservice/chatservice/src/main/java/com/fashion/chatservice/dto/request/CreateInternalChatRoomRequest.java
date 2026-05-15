package com.fashion.chatservice.dto.request;


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
public class CreateInternalChatRoomRequest {

    private UUID createdBy;

    private String roomType;
}
