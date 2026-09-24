package com.fashionsystem.fashion_system.entity;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Entity
@Table(name = "audit_outbox")
@Getter
@NoArgsConstructor
public class AuditOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "file_appended_at")
    private Instant fileAppendedAt;

    @Column(name = "kafka_published_at")
    private Instant kafkaPublishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    public static AuditOutbox pending(AuditEvent event, ObjectMapper objectMapper) {
        AuditOutbox outbox = new AuditOutbox();
        outbox.eventId = event.eventId();
        outbox.payload = objectMapper.valueToTree(event);
        outbox.occurredAt = event.occurredAt();
        outbox.createdAt = Instant.now();
        return outbox;
    }
}
