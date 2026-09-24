package com.fashionsystem.fashion_system.audit;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record AuditChange(String table, String rowId, AuditOperation operation,
                          List<String> changedFields, JsonNode oldValues, JsonNode newValues) {}
