package com.fashion.customerservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.customerservice.constant.ActionType;
import com.fashion.customerservice.constant.EntityType;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class ActivityLogResponse {
    private UUID id;
    private ActionType action;
    private EntityType entityType;
    private UUID entityId;
    private String metadata;
    private LocalDateTime createdAt;
}
