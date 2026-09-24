package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.AuditActorType;
import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditInfrastructure;
import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@AuditInfrastructure(reason = "Business audit persistence infrastructure")
public class AuditLogConsumerPersistence {
    private final AuditLogRepository repository;
    private final TransactionTemplate transaction;

    public AuditLogConsumerPersistence(AuditLogRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Returns false when this event has already been committed. */
    public boolean persist(AuditEvent event) {
        validate(event);
        try {
            return Boolean.TRUE.equals(transaction.execute(status -> {
                if (repository.existsByEventId(event.eventId())) return false;
                repository.saveAndFlush(AuditLog.builder()
                        .eventId(event.eventId())
                        .actorType(event.actorType())
                        .actorUserId(event.actorUserId())
                        .username(event.username() == null ? "SYSTEM" : event.username())
                        .action(event.action())
                        .requestId(event.requestId())
                        .method(event.method())
                        .path(event.path())
                        .jobName(event.jobName())
                        .rowCount(event.rowCount())
                        .changes(event.changes())
                        .occurredAt(event.occurredAt())
                        .ipAddress(event.ipAddress())
                        .userAgent(event.userAgent())
                        .createdAt(event.occurredAt() == null ? LocalDateTime.now(ZoneOffset.UTC)
                                : LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                        .build());
                return true;
            }));
        } catch (DataIntegrityViolationException duplicateCandidate) {
            if (repository.existsByEventId(event.eventId())) return false;
            throw duplicateCandidate;
        }
    }

    private static void validate(AuditEvent event) {
        if (event == null || event.schemaVersion() != 1 || event.eventId() == null
                || event.actorType() == null || event.action() == null || event.action().isBlank()
                || event.rowCount() != event.changes().size()
                || (event.actorType() == AuditActorType.EMPLOYEE && event.actorUserId() == null)
                || (event.actorType() == AuditActorType.SYSTEM && event.actorUserId() != null)) {
            throw new IllegalArgumentException("Invalid audit event");
        }
    }
}
