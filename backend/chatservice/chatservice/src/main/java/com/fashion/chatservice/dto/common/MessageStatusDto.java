package com.fashion.chatservice.dto.common;



import java.time.LocalDateTime;
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
public class MessageStatusDto {

    private UUID messageId;

    private UUID userId;

    private String status;

    private LocalDateTime updatedAt;
}
