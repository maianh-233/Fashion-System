package com.fashionsystem.fashion_system.dto;

import tools.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogDto(
        UUID id, UUID actorUserId, String username, String action, String entityType, UUID entityId,
        JsonNode oldData, JsonNode newData, JsonNode changedFields,
        String ipAddress, String userAgent, LocalDateTime createdAt) {
}
