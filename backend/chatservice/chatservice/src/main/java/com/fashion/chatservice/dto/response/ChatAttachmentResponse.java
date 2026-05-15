package com.fashion.chatservice.dto.response;


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
public class ChatAttachmentResponse {

    private UUID id;

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private LocalDateTime createdAt;
}
