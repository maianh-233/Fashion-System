package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import com.fashionsystem.fashion_system.audit.AuditActorType;
import com.fashionsystem.fashion_system.audit.AuditEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AuditLogConsumerIdempotencyTest {
    @Test
    void persistsAnEventOnlyOnce() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false, true);
        AuditLogConsumerPersistence service = new AuditLogConsumerPersistence(repository, manager);
        var event = systemEvent(eventId);

        assertThat(service.persist(event)).isTrue();
        assertThat(service.persist(event)).isFalse();
        verify(repository, times(1)).saveAndFlush(any(AuditLog.class));
        verify(manager, times(2)).commit(any());
    }

    @Test
    void uniqueConstraintRaceIsTreatedAsAlreadyPresent() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        UUID eventId = UUID.randomUUID();
        when(repository.existsByEventId(eventId)).thenReturn(false, true);
        when(repository.saveAndFlush(any(AuditLog.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(new AuditLogConsumerPersistence(repository, manager).persist(systemEvent(eventId))).isFalse();
        verify(manager).rollback(any());
    }

    private static AuditEvent systemEvent(UUID id) {
        return new AuditEvent(id, 1, AuditActorType.SYSTEM, null, "SYSTEM", "JOB_UPDATE", null,
                null, null, "nightly-job", null, null, 0, List.of(), Instant.now());
    }
}
