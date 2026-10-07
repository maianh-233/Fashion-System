package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Notification;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

class NotificationPersistenceServiceTest {

    @Test
    void duplicateEventRecipientIsPersistedAndReturnedOnlyOnce() {
        NotificationRepository repository = mock(NotificationRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));
        NotificationPersistenceService service = new NotificationPersistenceService(repository, transactionManager);
        UUID eventId = UUID.randomUUID();
        RealtimeIdentity recipient = RealtimeIdentity.user(UUID.randomUUID());
        NotificationEvent event = new NotificationEvent(
                1, eventId, List.of(recipient, recipient), UUID.randomUUID(), NotificationType.SYSTEM,
                "Title", "Message", "SYSTEM", UUID.randomUUID(), "/admin", Map.of(), Instant.now());
        when(repository.existsByEventIdAndRecipientTypeAndRecipientId(
                eventId, "USER", recipient.accountId())).thenReturn(false, true);
        when(repository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        var first = service.persist(event);
        var duplicate = service.persist(event);

        assertThat(first).singleElement().satisfies(persisted -> {
            assertThat(persisted.recipient()).isEqualTo(recipient);
            assertThat(persisted.notification().eventId()).isEqualTo(eventId);
        });
        assertThat(duplicate).isEmpty();
        verify(repository, times(1)).saveAndFlush(any(Notification.class));
    }
}
