package com.fashion.notificationservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.notificationservice.entity.enums.NotificationChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateResponse {

    private UUID id;

    private String code;

    private String titleTemplate;

    private String contentTemplate;

    private NotificationChannel channel;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}