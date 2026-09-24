package com.fashionsystem.fashion_system.audit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

/** One business request or system job, with all of its changed rows. */
public record AuditEvent(UUID eventId, int schemaVersion, AuditActorType actorType,
                         UUID actorUserId, String username, String action, String requestId,
                         String method, String path, String jobName, String ipAddress, String userAgent,
                         int rowCount, List<AuditChange> changes, Instant occurredAt) {
    public AuditEvent {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }

    /** Compatibility for producers being migrated to the request-level contract. */
    public static Builder builder() { return new Builder(); }

    /** Compatibility for the existing consumer until it reads the change set directly. */
    public String entityType() { return changes.isEmpty() ? null : changes.get(0).table(); }
    public UUID entityId() {
        if (changes.isEmpty()) return null;
        try { return UUID.fromString(changes.get(0).rowId()); }
        catch (IllegalArgumentException ex) { return null; }
    }
    public Map<String, Object> metadata() {
        if (changes.isEmpty() || changes.get(0).newValues() == null
                || !changes.get(0).newValues().isObject()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        JsonNode values = changes.get(0).newValues();
        values.propertyNames().forEach(name -> result.put(name, values.get(name)));
        return Map.copyOf(result);
    }
    public AuditCategory category() {
        return action != null && (action.startsWith("LOGIN_") || action.equals("LOGOUT"))
                ? AuditCategory.AUTH : AuditCategory.BUSINESS;
    }
    public AuditOutcome outcome() {
        return action != null && action.endsWith("FAILED") ? AuditOutcome.FAILURE : AuditOutcome.SUCCESS;
    }

    public static final class Builder {
        private UUID eventId = UUID.randomUUID();
        private AuditActorType actorType = AuditActorType.EMPLOYEE;
        private String action;
        private UUID actorUserId;
        private String username;
        private UUID entityId;
        private String entityType;
        private String method;
        private String path;
        private String ipAddress;
        private String userAgent;
        private Map<String, Object> metadata = Map.of();
        private Instant occurredAt = Instant.now();
        public Builder eventId(UUID v) { eventId=v; return this; }
        public Builder category(AuditCategory v) { return this; }
        public Builder action(String v) { action=v; return this; }
        public Builder outcome(AuditOutcome v) { return this; }
        public Builder actorUserId(UUID v) { actorUserId=v; return this; }
        public Builder username(String v) { username=v; return this; }
        public Builder entityId(UUID v) { entityId=v; return this; }
        public Builder entityType(String v) { entityType=v; return this; }
        public Builder method(String v) { method=v; return this; }
        public Builder path(String v) { path=v; return this; }
        public Builder ipAddress(String v) { ipAddress=v; return this; }
        public Builder userAgent(String v) { userAgent=v; return this; }
        public Builder metadata(Map<String, Object> v) { metadata=v; return this; }
        public Builder occurredAt(Instant v) { occurredAt=v; return this; }
        public AuditEvent build() {
            List<AuditChange> changes = entityId == null || entityType == null ? List.of() : List.of(
                    new AuditChange(entityType, entityId.toString(), AuditOperation.UPDATE, List.of(),
                            null, new ObjectMapper().valueToTree(metadata == null ? Map.of() : metadata)));
            return new AuditEvent(eventId, 1, actorType, actorUserId, username, action, null,
                    method, path, null, ipAddress, userAgent, changes.size(), changes, occurredAt);
        }
    }
}
