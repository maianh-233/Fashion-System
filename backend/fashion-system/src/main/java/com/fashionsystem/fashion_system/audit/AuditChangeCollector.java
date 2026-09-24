package com.fashionsystem.fashion_system.audit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Coalesces all flushes of a row into its transaction-level result. */
public final class AuditChangeCollector {
    private final Map<AuditChangeKey, PendingChange> changes = new LinkedHashMap<>();

    public void recordInsert(String table, String rowId, JsonNode newValues) {
        record(new AuditChangeKey(table, rowId), null, copy(newValues));
    }

    public void recordUpdate(String table, String rowId, JsonNode oldValues, JsonNode newValues) {
        record(new AuditChangeKey(table, rowId), copy(oldValues), copy(newValues));
    }

    public void recordDelete(String table, String rowId, JsonNode oldValues) {
        record(new AuditChangeKey(table, rowId), copy(oldValues), null);
    }

    public List<AuditChange> finish() {
        List<AuditChange> result = new ArrayList<>();
        changes.forEach((key, pending) -> {
            if (pending.oldValues == null && pending.newValues == null
                    || Objects.equals(pending.oldValues, pending.newValues)) return;
            AuditOperation operation = pending.oldValues == null ? AuditOperation.INSERT
                    : pending.newValues == null ? AuditOperation.DELETE : AuditOperation.UPDATE;
            result.add(new AuditChange(key.table(), key.rowId(), operation,
                    changedFields(pending.oldValues, pending.newValues),
                    AuditEventSanitizer.sanitize(pending.oldValues),
                    AuditEventSanitizer.sanitize(pending.newValues)));
        });
        return List.copyOf(result);
    }

    private void record(AuditChangeKey key, JsonNode oldValues, JsonNode newValues) {
        PendingChange previous = changes.get(key);
        if (previous == null) {
            changes.put(key, new PendingChange(oldValues, newValues));
        } else {
            previous.newValues = newValues;
        }
    }

    private static List<String> changedFields(JsonNode oldValues, JsonNode newValues) {
        Set<String> fields = new LinkedHashSet<>();
        if (oldValues != null && oldValues.isObject()) fields.addAll(oldValues.propertyNames());
        if (newValues != null && newValues.isObject()) fields.addAll(newValues.propertyNames());
        if (oldValues != null && newValues != null) {
            fields.removeIf(field -> Objects.equals(oldValues.get(field), newValues.get(field)));
        }
        return List.copyOf(fields);
    }

    private static JsonNode copy(JsonNode node) { return node == null ? null : node.deepCopy(); }

    private static final class PendingChange {
        private final JsonNode oldValues;
        private JsonNode newValues;

        private PendingChange(JsonNode oldValues, JsonNode newValues) {
            this.oldValues = oldValues;
            this.newValues = newValues;
        }
    }
}
