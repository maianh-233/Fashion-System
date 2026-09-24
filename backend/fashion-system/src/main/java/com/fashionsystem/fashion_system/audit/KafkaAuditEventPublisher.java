package com.fashionsystem.fashion_system.audit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
public class KafkaAuditEventPublisher implements AuditEventPublisher {
    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private final String topic;
    public KafkaAuditEventPublisher(KafkaTemplate<String, AuditEvent> kafkaTemplate,
                                    @Value("${audit.kafka.topic:audit.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }
    @Override
    public CompletionStage<Void> publish(AuditEvent event) {
        try {
            return kafkaTemplate.send(topic, event.eventId().toString(), event).thenApply(result -> null);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }
}
