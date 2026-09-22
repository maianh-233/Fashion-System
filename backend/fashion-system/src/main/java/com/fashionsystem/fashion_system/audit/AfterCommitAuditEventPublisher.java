package com.fashionsystem.fashion_system.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class AfterCommitAuditEventPublisher {
    private final AuditEventPublisher delegate;

    public void publishAfterCommit(AuditEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delegate.publish(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { delegate.publish(event); }
        });
    }
}
