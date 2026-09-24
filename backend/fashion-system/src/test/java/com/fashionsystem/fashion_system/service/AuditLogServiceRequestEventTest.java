package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.audit.AfterCommitAuditEventPublisher;
import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditOperation;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class AuditLogServiceRequestEventTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void update_event_keeps_before_after_and_changed_field() {
        AtomicReference<AuditEvent> published = new AtomicReference<>();
        AuditLogService service = service(published);

        service.record("EMPLOYEE_UPDATE", "users", UUID.randomUUID(),
                Map.of("name", "Old"), Map.of("name", "New"));

        var change = published.get().changes().get(0);
        assertThat(change.operation()).isEqualTo(AuditOperation.UPDATE);
        assertThat(change.changedFields()).containsExactly("name");
        assertThat(change.oldValues().get("name").asString()).isEqualTo("Old");
        assertThat(change.newValues().get("name").asString()).isEqualTo("New");
    }

    @Test
    void insert_event_keeps_new_values_without_an_old_snapshot() {
        AtomicReference<AuditEvent> published = new AtomicReference<>();
        AuditLogService service = service(published);

        service.record("EMPLOYEE_CREATE", "users", UUID.randomUUID(), null, Map.of("name", "New"));

        var change = published.get().changes().get(0);
        assertThat(change.operation()).isEqualTo(AuditOperation.INSERT);
        assertThat(change.changedFields()).containsExactly("name");
        assertThat(change.oldValues()).isNull();
        assertThat(change.newValues().get("name").asString()).isEqualTo("New");
    }

    @Test
    void delete_event_keeps_old_values_without_a_new_snapshot() {
        AtomicReference<AuditEvent> published = new AtomicReference<>();
        AuditLogService service = service(published);

        service.record("EMPLOYEE_DELETE", "users", UUID.randomUUID(), Map.of("name", "Old"), null);

        var change = published.get().changes().get(0);
        assertThat(change.operation()).isEqualTo(AuditOperation.DELETE);
        assertThat(change.changedFields()).containsExactly("name");
        assertThat(change.oldValues().get("name").asString()).isEqualTo("Old");
        assertThat(change.newValues()).isNull();
    }

    private AuditLogService service(AtomicReference<AuditEvent> published) {
        var principal = new AuthenticatedUser(UUID.randomUUID(), "auditor");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        return new AuditLogService(null, new ObjectMapper(), new AfterCommitAuditEventPublisher(published::set));
    }
}
