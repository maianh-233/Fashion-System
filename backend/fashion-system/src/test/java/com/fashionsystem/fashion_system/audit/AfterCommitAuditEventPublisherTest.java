package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AfterCommitAuditEventPublisherTest {
    @Test
    void publishesOnlyFromAfterCommitCallback() {
        AuditEventPublisher delegate = mock(AuditEventPublisher.class);
        AfterCommitAuditEventPublisher publisher = new AfterCommitAuditEventPublisher(delegate);
        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID())
                .action("CREATE").build();
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishAfterCommit(event);
            verifyNoInteractions(delegate);
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(delegate).publish(event);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }
}
