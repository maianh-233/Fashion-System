package com.fashionsystem.fashion_system.audit.spool;

import java.nio.file.Path;
import java.time.ZoneId;

public record AuditSpoolProperties(Path directory, String instanceId, ZoneId zoneId) {
    public AuditSpoolProperties {
        if (directory == null || instanceId == null || !instanceId.matches("[A-Za-z0-9_-]+") || zoneId == null) {
            throw new IllegalArgumentException("Invalid audit spool configuration");
        }
    }
}
