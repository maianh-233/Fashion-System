package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaNotificationPublisherTest {

    @Test
    void mapsAndDeduplicatesRecipientsBeforePublishing() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, NotificationEvent> template = mock(KafkaTemplate.class);
        when(template.send(eq("fashion.notifications"), any(String.class), any(NotificationEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        Instant now = Instant.parse("2026-10-08T10:00:00Z");
        KafkaNotificationPublisher publisher = new KafkaNotificationPublisher(
                template, "fashion.notifications", Clock.fixed(now, ZoneOffset.UTC));
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        NotificationRequest request = request(List.of(
                RealtimeIdentity.user(userId),
                RealtimeIdentity.user(userId),
                RealtimeIdentity.customer(customerId)));

        publisher.publish(request);

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(template).send(eq("fashion.notifications"), any(String.class), event.capture());
        assertThat(event.getValue().recipientIdentities())
                .containsExactly(RealtimeIdentity.user(userId), RealtimeIdentity.customer(customerId));
        assertThat(event.getValue().eventId()).isNotNull();
        assertThat(event.getValue().occurredAt()).isEqualTo(now);
    }

    @Test
    void rejectsEmptyRecipientsButDoesNotPropagateAsyncKafkaFailure() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, NotificationEvent> template = mock(KafkaTemplate.class);
        KafkaNotificationPublisher publisher = new KafkaNotificationPublisher(
                template, "fashion.notifications", Clock.systemUTC());

        assertThatThrownBy(() -> publisher.publish(request(List.of())))
                .isInstanceOf(IllegalArgumentException.class);

        CompletableFuture failed = CompletableFuture.failedFuture(new IllegalStateException("Kafka down"));
        when(template.send(eq("fashion.notifications"), any(String.class), any(NotificationEvent.class)))
                .thenReturn(failed);
        assertThatCode(() -> publisher.publish(request(List.of(RealtimeIdentity.user(UUID.randomUUID())))))
                .doesNotThrowAnyException();
    }

    private NotificationRequest request(List<RealtimeIdentity> recipients) {
        return new NotificationRequest(
                recipients,
                UUID.randomUUID(),
                NotificationType.SYSTEM,
                "Title",
                "Message",
                "SYSTEM",
                UUID.randomUUID(),
                "/admin",
                Map.of("source", "test"));
    }
}
