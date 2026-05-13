package com.fashion.notificationservice.dto.request;

import com.fashion.notificationservice.entity.enums.NotificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateTemplateRequest {

    @NotBlank(message = "Code is required")
    private String code;

    private String titleTemplate;

    @NotBlank(message = "Content template is required")
    private String contentTemplate;

    @NotNull(message = "Channel is required")
    private NotificationChannel channel;
}