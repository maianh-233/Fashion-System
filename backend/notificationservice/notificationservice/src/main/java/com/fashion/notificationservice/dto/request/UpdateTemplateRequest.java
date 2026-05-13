package com.fashion.notificationservice.dto.request;

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
public class UpdateTemplateRequest {

    private String titleTemplate;

    private String contentTemplate;

    private NotificationChannel channel;

    private Boolean active;
}