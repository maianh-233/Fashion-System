package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaAuditEventPublisherTest {
    @Test
    @SuppressWarnings("unchecked")
    void kafka_result_remains_pending_until_broker_acknowledges() {
        KafkaTemplate<String, AuditEvent> template = org.mockito.Mockito.mock(KafkaTemplate.class);
        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).action("CREATE").build();
        CompletableFuture<SendResult<String, AuditEvent>> broker = new CompletableFuture<>();
        when(template.send("audit.events", event.eventId().toString(), event)).thenReturn(broker);

        var delivery = new KafkaAuditEventPublisher(template, "audit.events").publish(event).toCompletableFuture();
        assertThat(delivery).isNotDone();

        broker.complete(null);
        assertThat(delivery).isCompletedWithValue(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void broker_rejection_is_returned_to_caller() {
        KafkaTemplate<String, AuditEvent> template = org.mockito.Mockito.mock(KafkaTemplate.class);
        AuditEvent event = AuditEvent.builder().eventId(UUID.randomUUID()).action("CREATE").build();
        CompletableFuture<SendResult<String, AuditEvent>> broker = new CompletableFuture<>();
        when(template.send("audit.events", event.eventId().toString(), event)).thenReturn(broker);

        var delivery = new KafkaAuditEventPublisher(template, "audit.events").publish(event).toCompletableFuture();
        broker.completeExceptionally(new IllegalStateException("broker down"));

        assertThatThrownBy(delivery::join).hasRootCauseMessage("broker down");
    }

    @Test
    void disabled_kafka_does_not_acknowledge_delivery() {
        var delivery = new NoopAuditEventPublisher().publish(
                AuditEvent.builder().action("CREATE").build()).toCompletableFuture();

        assertThatThrownBy(delivery::join).hasRootCauseInstanceOf(IllegalStateException.class);
    }
}
