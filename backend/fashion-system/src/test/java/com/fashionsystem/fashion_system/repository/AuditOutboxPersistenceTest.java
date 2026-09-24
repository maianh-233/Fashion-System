package com.fashionsystem.fashion_system.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.audit.AuditActorType;
import com.fashionsystem.fashion_system.audit.AuditChange;
import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditOperation;
import com.fashionsystem.fashion_system.entity.AuditOutbox;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

class AuditOutboxPersistenceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void pending_outbox_retains_one_event_with_both_row_changes() {
        AuditEvent event = event();

        AuditOutbox outbox = AuditOutbox.pending(event, objectMapper);

        assertThat(outbox.getEventId()).isEqualTo(event.eventId());
        assertThat(outbox.getPayload().get("rowCount").asInt()).isEqualTo(2);
        assertThat(outbox.getPayload().get("changes").size()).isEqualTo(2);
        assertThat(outbox.getPayload().get("changes").get(0).get("rowId").asString()).isEqualTo("u-1");
        assertThat(outbox.getPayload().get("changes").get(1).get("rowId").asString()).isEqualTo("u-1:r-2");
        assertThat(outbox.getFileAppendedAt()).isNull();
        assertThat(outbox.getKafkaPublishedAt()).isNull();
    }

    @Nested
    @SpringBootTest
    @Transactional
    class PostgresRoundTrip {
        @Autowired AuditOutboxRepository repository;
        @Autowired EntityManager entityManager;
        @Autowired ObjectMapper objectMapper;

        @Test
        void stores_one_outbox_payload_for_a_multi_row_event() {
            AuditEvent event = event();
            repository.saveAndFlush(AuditOutbox.pending(event, objectMapper));
            entityManager.clear();

            AuditOutbox found = repository.findByEventId(event.eventId()).orElseThrow();
            assertThat(found.getPayload()).isEqualTo(objectMapper.valueToTree(event));
            assertThat(found.getFileAppendedAt()).isNull();
            assertThat(found.getKafkaPublishedAt()).isNull();
        }
    }

    private AuditEvent event() {
        return new AuditEvent(UUID.randomUUID(), 1, AuditActorType.EMPLOYEE, UUID.randomUUID(),
                "admin", "EMPLOYEE_UPDATE", "request-1", "PATCH", "/api/employees/u-1", null,
                "127.0.0.1", "test", 2, List.of(
                        new AuditChange("users", "u-1", AuditOperation.UPDATE, List.of("name"),
                                JsonNodeFactory.instance.objectNode().put("name", "Old"),
                                JsonNodeFactory.instance.objectNode().put("name", "New")),
                        new AuditChange("user_roles", "u-1:r-2", AuditOperation.INSERT, List.of("role_id"),
                                null, JsonNodeFactory.instance.objectNode().put("role_id", "r-2"))),
                Instant.parse("2026-09-24T00:00:00Z"));
    }
}
