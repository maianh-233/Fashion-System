package com.fashionsystem.fashion_system.audit;

import tools.jackson.databind.JsonNode;

/** Bridge for native writes which do not invoke Hibernate entity callbacks. */
public final class BulkAuditRecorder {
    private BulkAuditRecorder() {}

    public static void record(String table, String rowId, AuditOperation operation, JsonNode oldValues, JsonNode newValues) {
        if (table == null || table.isBlank() || rowId == null || rowId.isBlank())
            throw new IllegalArgumentException("Bulk audit requires an exact table and row ID");
        var collector = AuditCaptureScope.current().orElseThrow(
                () -> new IllegalStateException("Bulk mutation requires an active business audit scope"));
        switch (operation) {
            case INSERT -> collector.recordInsert(table, rowId, newValues);
            case UPDATE -> collector.recordUpdate(table, rowId, oldValues, newValues);
            case DELETE -> collector.recordDelete(table, rowId, oldValues);
        }
    }
}
