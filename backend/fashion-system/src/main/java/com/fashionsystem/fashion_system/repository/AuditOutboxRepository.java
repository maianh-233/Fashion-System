package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AuditOutbox;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditOutboxRepository extends JpaRepository<AuditOutbox, UUID> {
    Optional<AuditOutbox> findByEventId(UUID eventId);

    long countByFileAppendedAtIsNullOrKafkaPublishedAtIsNull();

    @Query("""
            select count(o) from AuditOutbox o
            where o.occurredAt >= :start and o.occurredAt < :end
              and (o.fileAppendedAt is null or o.kafkaPublishedAt is null)
            """)
    long countUnfinishedBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = """
            SELECT * FROM audit_outbox
            WHERE (file_appended_at IS NULL OR kafka_published_at IS NULL)
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY occurred_at, event_id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AuditOutbox> lockPending(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
