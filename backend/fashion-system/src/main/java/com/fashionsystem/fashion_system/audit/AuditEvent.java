package com.fashionsystem.fashion_system.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(UUID eventId, AuditCategory category, String action, AuditOutcome outcome,
                         UUID actorUserId, String username, UUID entityId, String entityType,
                         String method, String path, Integer status, Long durationMs,
                         String ipAddress, String userAgent, Map<String, Object> metadata,
                         Instant occurredAt) {
    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private UUID eventId = UUID.randomUUID();
        private AuditCategory category;
        private String action;
        private AuditOutcome outcome = AuditOutcome.SUCCESS;
        private UUID actorUserId;
        private String username;
        private UUID entityId;
        private String entityType;
        private String method;
        private String path;
        private Integer status;
        private Long durationMs;
        private String ipAddress;
        private String userAgent;
        private Map<String, Object> metadata = Map.of();
        private Instant occurredAt = Instant.now();
        public Builder eventId(UUID v) { eventId=v; return this; }
        public Builder category(AuditCategory v) { category=v; return this; }
        public Builder action(String v) { action=v; return this; }
        public Builder outcome(AuditOutcome v) { outcome=v; return this; }
        public Builder actorUserId(UUID v) { actorUserId=v; return this; }
        public Builder username(String v) { username=v; return this; }
        public Builder entityId(UUID v) { entityId=v; return this; }
        public Builder entityType(String v) { entityType=v; return this; }
        public Builder method(String v) { method=v; return this; }
        public Builder path(String v) { path=v; return this; }
        public Builder status(Integer v) { status=v; return this; }
        public Builder durationMs(Long v) { durationMs=v; return this; }
        public Builder ipAddress(String v) { ipAddress=v; return this; }
        public Builder userAgent(String v) { userAgent=v; return this; }
        public Builder metadata(Map<String, Object> v) { metadata=v; return this; }
        public Builder occurredAt(Instant v) { occurredAt=v; return this; }
        public AuditEvent build() { return new AuditEvent(eventId, category, action, outcome, actorUserId, username, entityId, entityType, method, path, status, durationMs, ipAddress, userAgent, metadata == null ? Map.of() : Map.copyOf(metadata), occurredAt); }
    }
}
