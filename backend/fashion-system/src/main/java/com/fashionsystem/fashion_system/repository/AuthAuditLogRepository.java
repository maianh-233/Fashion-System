package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AuthAuditLog;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho AuthAuditLog.
 */
public interface AuthAuditLogRepository extends BaseRepository<AuthAuditLog, UUID> {
    java.util.List<AuthAuditLog> findAllByOrderByCreatedAtDesc();

    String HISTORY_FILTER = """
            from auth_audit_logs a
            where a.action in ('LOGIN_SUCCESS', 'LOGIN_FAILED', 'LOGOUT')
              and (cast(:actorUserId as uuid) is null or a.user_id = :actorUserId)
              and (cast(:action as text) is null or a.action = :action)
              and (cast(:fromAt as timestamp) is null or a.created_at >= :fromAt)
              and (cast(:toAt as timestamp) is null or a.created_at < :toAt)
            """;

    @org.springframework.data.jpa.repository.Query(
            value = "select a.* " + HISTORY_FILTER + " order by a.created_at desc nulls last, a.id desc",
            countQuery = "select count(*) " + HISTORY_FILTER, nativeQuery = true)
    org.springframework.data.domain.Page<AuthAuditLog> searchHistory(
            @org.springframework.data.repository.query.Param("actorUserId") UUID actorUserId,
            @org.springframework.data.repository.query.Param("action") String action,
            @org.springframework.data.repository.query.Param("fromAt") java.time.LocalDateTime fromAt,
            @org.springframework.data.repository.query.Param("toAt") java.time.LocalDateTime toAt,
            org.springframework.data.domain.Pageable pageable);
}
