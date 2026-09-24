package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fashionsystem.fashion_system.audit.AuditActorType;
import com.fashionsystem.fashion_system.audit.AuditChange;
import java.time.Instant;
import java.util.List;

public record SystemAuditLogDto(UUID id, UUID eventId, String action,
        AuditActorType actorType, UUID actorUserId, String username,
        String requestId, String method, String path, String jobName,
        Integer rowCount, List<AuditChange> changes, String ipAddress, String userAgent,
        Instant occurredAt, LocalDateTime createdAt, boolean migratedFromAuthAudit, String detail) {}
