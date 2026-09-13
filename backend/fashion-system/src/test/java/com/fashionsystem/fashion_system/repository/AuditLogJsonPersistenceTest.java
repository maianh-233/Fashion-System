package com.fashionsystem.fashion_system.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.entity.AuditLog;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;

@SpringBootTest
@Transactional
class AuditLogJsonPersistenceTest {
    @Autowired EntityManager entityManager;

    @Test
    void persistsAndReadsJackson3JsonNodes() {
        var newData = JsonNodeFactory.instance.objectNode().put("email", "employee@example.com");
        var changedFields = JsonNodeFactory.instance.arrayNode().add("email");
        var auditLog = AuditLog.builder()
                .actorUserId(UUID.randomUUID())
                .username("admin")
                .action("CREATE")
                .entityType("EMPLOYEE")
                .entityId(UUID.randomUUID())
                .newData(newData)
                .changedFields(changedFields)
                .createdAt(LocalDateTime.now())
                .build();

        entityManager.persist(auditLog);
        entityManager.flush();
        UUID id = auditLog.getId();
        entityManager.clear();

        AuditLog reloaded = entityManager.find(AuditLog.class, id);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getNewData().get("email").asString())
                .isEqualTo("employee@example.com");
        assertThat(reloaded.getChangedFields().get(0).asString()).isEqualTo("email");
    }
}
