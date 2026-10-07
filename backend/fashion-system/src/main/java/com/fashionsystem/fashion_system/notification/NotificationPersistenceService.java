package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.entity.Notification;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationPersistenceService {

    private final NotificationRepository repository;
    private final TransactionTemplate transaction;

    public NotificationPersistenceService(
            NotificationRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Persists each unique event-recipient pair; returned rows are the only rows eligible for realtime push. */
    public List<PersistedNotification> persist(NotificationEvent event) {
        validate(event);
        List<PersistedNotification> created = new ArrayList<>();
        for (RealtimeIdentity recipient : new LinkedHashSet<>(event.recipientIdentities())) {
            PersistedNotification persisted = persistOne(event, recipient);
            if (persisted != null) created.add(persisted);
        }
        return List.copyOf(created);
    }

    private PersistedNotification persistOne(NotificationEvent event, RealtimeIdentity recipient) {
        try {
            return transaction.execute(status -> {
                if (repository.existsByEventIdAndRecipientTypeAndRecipientId(
                        event.eventId(), recipient.accountType().name(), recipient.accountId())) {
                    return null;
                }
                Notification saved = repository.saveAndFlush(Notification.builder()
                        .eventId(event.eventId())
                        .recipientId(recipient.accountId())
                        .recipientType(recipient.accountType().name())
                        .actorUserId(event.actorUserId())
                        .channel("NOTIFICATION")
                        .type(event.type())
                        .title(event.title())
                        .content(event.message())
                        .referenceType(event.entityType())
                        .referenceId(event.entityId())
                        .actionUrl(event.actionUrl())
                        .metadata(event.metadata())
                        .status("CREATED")
                        .createdAt(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC))
                        .build());
                return new PersistedNotification(recipient, NotificationResponse.from(saved));
            });
        } catch (DataIntegrityViolationException duplicateCandidate) {
            if (repository.existsByEventIdAndRecipientTypeAndRecipientId(
                    event.eventId(), recipient.accountType().name(), recipient.accountId())) {
                return null;
            }
            throw duplicateCandidate;
        }
    }

    private void validate(NotificationEvent event) {
        if (event == null || event.schemaVersion() != 1 || event.eventId() == null
                || event.recipientIdentities() == null || event.recipientIdentities().isEmpty()
                || event.recipientIdentities().stream().anyMatch(Objects::isNull)
                || blank(event.type()) || blank(event.title()) || blank(event.message())
                || event.occurredAt() == null) {
            throw new IllegalArgumentException("Invalid notification event");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
