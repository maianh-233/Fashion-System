package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.realtime.RealtimeEvent;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.realtime.RealtimePublisher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class NotificationConsumerTest {

    @Test
    void persistsBeforeRealtimePushAndAcknowledges() {
        NotificationPersistenceService persistence = mock(NotificationPersistenceService.class);
        RealtimePublisher realtime = mock(RealtimePublisher.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        NotificationConsumer consumer = new NotificationConsumer(persistence, realtime);
        NotificationEvent event = event();
        RealtimeIdentity recipient = event.recipientIdentities().getFirst();
        NotificationResponse response = response(event.eventId());
        when(persistence.persist(event)).thenReturn(List.of(new PersistedNotification(recipient, response)));

        consumer.consume(event, acknowledgment);

        var order = inOrder(persistence, realtime, acknowledgment);
        order.verify(persistence).persist(event);
        order.verify(realtime).sendToIdentity(
                org.mockito.ArgumentMatchers.eq(recipient), any(RealtimeEvent.class));
        order.verify(acknowledgment).acknowledge();
    }

    @Test
    void duplicateCreatesNoPushAndRealtimeFailureStillAcknowledges() {
        NotificationPersistenceService persistence = mock(NotificationPersistenceService.class);
        RealtimePublisher realtime = mock(RealtimePublisher.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        NotificationConsumer consumer = new NotificationConsumer(persistence, realtime);
        NotificationEvent event = event();

        when(persistence.persist(event)).thenReturn(List.of());
        consumer.consume(event, acknowledgment);
        verify(realtime, never()).sendToIdentity(any(), any());
        verify(acknowledgment).acknowledge();

        NotificationResponse response = response(event.eventId());
        when(persistence.persist(event)).thenReturn(List.of(
                new PersistedNotification(event.recipientIdentities().getFirst(), response)));
        org.mockito.Mockito.doThrow(new IllegalStateException("socket down"))
                .when(realtime).sendToIdentity(any(), any());

        assertThatCode(() -> consumer.consume(event, acknowledgment)).doesNotThrowAnyException();
        verify(acknowledgment, org.mockito.Mockito.times(2)).acknowledge();
    }

    private NotificationEvent event() {
        return new NotificationEvent(
                1,
                UUID.randomUUID(),
                List.of(RealtimeIdentity.user(UUID.randomUUID())),
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                "Title",
                "Message",
                "SYSTEM",
                UUID.randomUUID(),
                "/admin",
                Map.of(),
                Instant.now());
    }

    private NotificationResponse response(UUID eventId) {
        return new NotificationResponse(
                UUID.randomUUID(), eventId, NotificationType.SYSTEM, "Title", "Message",
                "SYSTEM", UUID.randomUUID(), "/admin", Map.of(), Instant.now(), false);
    }
}
