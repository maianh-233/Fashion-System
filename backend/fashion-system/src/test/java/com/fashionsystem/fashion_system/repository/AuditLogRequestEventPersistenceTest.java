package com.fashionsystem.fashion_system.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.audit.AuditActorType;
import com.fashionsystem.fashion_system.audit.AuditChange;
import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditOperation;
import com.fashionsystem.fashion_system.entity.AuditLog;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;

class AuditLogRequestEventPersistenceTest {
    @Test
    void legacy_single_entity_event_keeps_metadata_for_current_consumer() {
        AuditEvent event = AuditEvent.builder().action("EMPLOYEE_UPDATE")
                .entityType("users").entityId(UUID.randomUUID())
                .metadata(Map.of("changedFields", List.of("name"))).build();

        assertThat(event.metadata().get("changedFields")).isNotNull();
    }

    @Test
    void request_audit_represents_two_changed_rows() {
        AuditLog auditLog = auditLogWithTwoChanges();

        assertThat(auditLog.getRowCount()).isEqualTo(2);
        assertThat(auditLog.getChanges()).hasSize(2);
        assertThat(auditLog.getChanges().get(0).rowId()).isEqualTo("u-1");
        assertThat(auditLog.getChanges().get(1).rowId()).isEqualTo("u-1:r-2");
        assertThat(auditLog.getRequestId()).isEqualTo("request-1");
    }

    @Nested
    @SpringBootTest
    @Transactional
    class PostgresRoundTrip {
        @Autowired AuditLogRepository repository;
        @Autowired EntityManager entityManager;

        @Test
        void stores_request_level_audit_with_row_count_and_changes() {
            AuditLog saved = repository.save(auditLogWithTwoChanges());
            entityManager.flush();
            entityManager.clear();

            AuditLog found = repository.findByEventId(saved.getEventId()).orElseThrow();
            assertThat(found.getRowCount()).isEqualTo(2);
            assertThat(found.getChanges()).hasSize(2);
            assertThat(found.getChanges().get(0).rowId()).isEqualTo("u-1");
            assertThat(found.getChanges().get(1).rowId()).isEqualTo("u-1:r-2");
            assertThat(found.getRequestId()).isEqualTo("request-1");
        }
    }

    private AuditLog auditLogWithTwoChanges() {
        return AuditLog.builder()
                .eventId(UUID.randomUUID())
                .actorType(AuditActorType.EMPLOYEE)
                .actorUserId(UUID.randomUUID())
                .username("admin")
                .action("EMPLOYEE_UPDATE")
                .requestId("request-1")
                .method("PATCH")
                .path("/api/employees/u-1")
                .rowCount(2)
                .changes(List.of(
                        new AuditChange("users", "u-1", AuditOperation.UPDATE, List.of("name"),
                                JsonNodeFactory.instance.objectNode().put("name", "Old"),
                                JsonNodeFactory.instance.objectNode().put("name", "New")),
                        new AuditChange("user_roles", "u-1:r-2", AuditOperation.INSERT, List.of("role_id"),
                                null, JsonNodeFactory.instance.objectNode().put("role_id", "r-2"))))
                .occurredAt(Instant.parse("2026-09-24T00:00:00Z"))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
