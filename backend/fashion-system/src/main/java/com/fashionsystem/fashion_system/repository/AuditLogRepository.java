package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Chỉ cung cấp insert và read; không expose delete/update cho audit log. */
public interface AuditLogRepository extends org.springframework.data.repository.Repository<AuditLog, UUID> {
    <S extends AuditLog> S save(S entity);
    boolean existsByEventId(UUID eventId);
    <S extends AuditLog> S saveAndFlush(S entity);
    @Query("select a.eventId from AuditLog a where a.eventId in :eventIds")
    List<UUID> findExistingEventIds(@Param("eventIds") Collection<UUID> eventIds);
    @Query("select count(a) from AuditLog a where a.occurredAt >= :start and a.occurredAt < :end")
    long countOccurredBetween(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);
    Optional<AuditLog> findByEventId(UUID eventId);
    java.util.List<AuditLog> findAllByOrderByCreatedAtDesc();

    @Query("""
            select a from AuditLog a
            where (:actorUserId is null or a.actorUserId = :actorUserId)
              and (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:username is null or lower(a.username) like lower(concat('%', :username, '%')))
              and (:fromAt is null or a.createdAt >= :fromAt)
              and (:toAt is null or a.createdAt < :toAt)
            """)
    Page<AuditLog> search(
            @Param("actorUserId") UUID actorUserId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("username") String username,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable);
}
