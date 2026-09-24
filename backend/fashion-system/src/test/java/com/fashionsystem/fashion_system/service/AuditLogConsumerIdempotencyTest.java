package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogConsumerIdempotencyTest {
    @Test
    void persistsAnEventOnlyOnce() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false, true);
        AuditLogConsumerPersistence service = new AuditLogConsumerPersistence(repository);
        var event = com.fashionsystem.fashion_system.audit.AuditEvent.builder()
                .eventId(eventId)
                .action("CREATE").build();

        assertThat(service.persist(event)).isTrue();
        assertThat(service.persist(event)).isFalse();
        verify(repository, times(1)).save(any(AuditLog.class));
    }
}
