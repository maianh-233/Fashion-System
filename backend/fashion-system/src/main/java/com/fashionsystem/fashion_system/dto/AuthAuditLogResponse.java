package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthAuditLogResponse(UUID id, UUID userId, String action, String description,
        String ipAddress, String userAgent, LocalDateTime createdAt) {}
