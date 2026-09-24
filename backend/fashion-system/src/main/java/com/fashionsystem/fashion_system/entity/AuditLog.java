package com.fashionsystem.fashion_system.entity;

import com.fashionsystem.fashion_system.audit.AuditActorType;
import com.fashionsystem.fashion_system.audit.AuditChange;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Nhật ký nghiệp vụ append-only. Không dùng entity này cho thao tác update/delete. */
@Entity
@Table(name = "audit_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Column(name = "event_id", unique = true)
    private UUID eventId;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 16)
    private AuditActorType actorType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "method", length = 16)
    private String method;

    @Column(name = "path", columnDefinition = "text")
    private String path;

    @Column(name = "job_name", length = 128)
    private String jobName;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "changes", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private List<AuditChange> changes;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "old_data", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private JsonNode oldData;

    @Column(name = "new_data", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private JsonNode newData;

    @Column(name = "changed_fields", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private JsonNode changedFields;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
