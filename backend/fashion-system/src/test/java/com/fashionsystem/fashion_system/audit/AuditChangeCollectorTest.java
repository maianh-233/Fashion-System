package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AuditChangeCollectorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuditChangeCollector collector = new AuditChangeCollector();

    @Test
    void mergesRepeatedUpdatesToFirstOldAndLastNew() {
        collector.recordUpdate("users", "u1", json("{\"name\":\"A\"}"), json("{\"name\":\"B\"}"));
        collector.recordUpdate("users", "u1", json("{\"name\":\"B\"}"), json("{\"name\":\"C\"}"));

        AuditChange change = collector.finish().getFirst();
        assertThat(change.operation()).isEqualTo(AuditOperation.UPDATE);
        assertThat(change.oldValues().get("name").asText()).isEqualTo("A");
        assertThat(change.newValues().get("name").asText()).isEqualTo("C");
        assertThat(change.changedFields()).containsExactly("name");
    }

    @Test
    void insertThenDeleteHasNoFinalChange() {
        collector.recordInsert("users", "u1", json("{\"name\":\"A\"}"));
        collector.recordDelete("users", "u1", json("{\"name\":\"A\"}"));
        assertThat(collector.finish()).isEmpty();
    }

    @Test
    void insertThenUpdateIsOneInsertWithFinalValues() {
        collector.recordInsert("users", "u1", json("{\"name\":\"A\"}"));
        collector.recordUpdate("users", "u1", json("{\"name\":\"A\"}"), json("{\"name\":\"B\"}"));
        AuditChange change = collector.finish().getFirst();
        assertThat(change.operation()).isEqualTo(AuditOperation.INSERT);
        assertThat(change.oldValues()).isNull();
        assertThat(change.newValues().get("name").asText()).isEqualTo("B");
    }

    @Test
    void updateThenDeleteKeepsOriginalState() {
        collector.recordUpdate("users", "u1", json("{\"name\":\"A\"}"), json("{\"name\":\"B\"}"));
        collector.recordDelete("users", "u1", json("{\"name\":\"B\"}"));
        AuditChange change = collector.finish().getFirst();
        assertThat(change.operation()).isEqualTo(AuditOperation.DELETE);
        assertThat(change.oldValues().get("name").asText()).isEqualTo("A");
        assertThat(change.newValues()).isNull();
    }

    @Test
    void returnsNoChangeWhenFinalStateMatchesOriginal() {
        collector.recordUpdate("users", "u1", json("{\"name\":\"A\"}"), json("{\"name\":\"B\"}"));
        collector.recordUpdate("users", "u1", json("{\"name\":\"B\"}"), json("{\"name\":\"A\"}"));
        assertThat(collector.finish()).isEmpty();
    }

    @Test
    void keepsIdenticalRowIdsInDifferentTablesSeparate() {
        collector.recordInsert("users", "1", json("{\"name\":\"A\"}"));
        collector.recordInsert("orders", "1", json("{\"status\":\"NEW\"}"));
        assertThat(collector.finish()).extracting(AuditChange::table).containsExactly("users", "orders");
    }

    @Test
    void redactsCapturedValuesWithoutHidingChangedField() {
        collector.recordUpdate("users", "u1", json("{\"passwordHash\":\"old\"}"), json("{\"passwordHash\":\"new\"}"));
        AuditChange change = collector.finish().getFirst();
        assertThat(change.changedFields()).containsExactly("passwordHash");
        assertThat(change.oldValues().get("passwordHash").asText()).isEqualTo("[REDACTED]");
        assertThat(change.newValues().get("passwordHash").asText()).isEqualTo("[REDACTED]");
    }

    private JsonNode json(String value) { return mapper.readTree(value); }
}
