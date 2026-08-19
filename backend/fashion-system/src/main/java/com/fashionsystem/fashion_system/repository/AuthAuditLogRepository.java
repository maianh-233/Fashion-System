package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho AuthAuditLog.
 */
public interface AuthAuditLogRepository extends BaseRepository<AuthAuditLog, UUID> {
}
