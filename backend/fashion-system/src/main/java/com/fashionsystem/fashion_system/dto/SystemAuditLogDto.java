package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SystemAuditLogDto(UUID id, String category, String level, String action, String detail,
                                UUID actorUserId, String username, String entityType, UUID entityId,
                                String ipAddress, String userAgent, LocalDateTime createdAt) {}
