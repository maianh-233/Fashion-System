package com.fashionsystem.fashion_system.audit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
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
        if (changes.isEmpty()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        AuditChange change = changes.get(0);
        if (change.oldValues() != null) result.put("oldData", change.oldValues());
        if (change.newValues() != null) result.put("newData", change.newValues());
        result.put("changedFields", change.changedFields());
        return Map.copyOf(result);
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
        public Builder action(String v) { action=v; return this; }
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
            ObjectMapper mapper = new ObjectMapper();
            JsonNode oldValues = value(metadata == null ? null : metadata.get("oldData"), mapper);
            JsonNode newValues = value(metadata == null ? null : metadata.get("newData"), mapper);
            JsonNode fields = value(metadata == null ? null : metadata.get("changedFields"), mapper);
            List<String> changedFields = fields == null || !fields.isArray() ? List.of()
                    : IntStream.range(0, fields.size()).mapToObj(i -> fields.get(i).asString()).toList();
            AuditOperation operation = oldValues == null && newValues != null ? AuditOperation.INSERT
                    : oldValues != null && newValues == null ? AuditOperation.DELETE : AuditOperation.UPDATE;
            List<AuditChange> changes = entityId == null || entityType == null ? List.of() : List.of(
                    new AuditChange(entityType, entityId.toString(), operation, changedFields,
                            oldValues, newValues));
            return new AuditEvent(eventId, 1, actorType, actorUserId, username, action, null,
                    method, path, null, ipAddress, userAgent, changes.size(), changes, occurredAt);
        }
        private JsonNode value(Object raw, ObjectMapper mapper) {
            return raw == null ? null : raw instanceof JsonNode node ? node : mapper.valueToTree(raw);
        }
    }
}
