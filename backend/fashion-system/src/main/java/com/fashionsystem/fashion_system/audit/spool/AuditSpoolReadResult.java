package com.fashionsystem.fashion_system.audit.spool;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuditSpoolReadResult(boolean valid, List<AuditEvent> events, Set<UUID> duplicateEventIds) { }
