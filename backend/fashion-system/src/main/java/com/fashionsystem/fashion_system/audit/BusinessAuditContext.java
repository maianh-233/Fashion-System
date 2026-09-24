package com.fashionsystem.fashion_system.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BusinessAuditContext(AuditActorType actorType, UUID actorUserId, String username,
                                   String action, String requestId, String method, String path,
                                   String jobName, String ipAddress, String userAgent, Instant occurredAt) {
    public AuditEvent event(List<AuditChange> changes) {
        return new AuditEvent(UUID.randomUUID(), 1, actorType, actorUserId, username, action,
                requestId, method, path, jobName, ipAddress, userAgent,
                changes.size(), changes, occurredAt);
    }
}
