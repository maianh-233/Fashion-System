package com.fashionsystem.fashion_system.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
public class KafkaAuditEventPublisher implements AuditEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaAuditEventPublisher.class);
    private final KafkaTemplate<String, AuditEvent> kafkaTemplate;
    private final String topic;
    public KafkaAuditEventPublisher(KafkaTemplate<String, AuditEvent> kafkaTemplate,
                                    @Value("${audit.kafka.topic:audit.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }
    @Override
    public void publish(AuditEvent event) {
        try {
            kafkaTemplate.send(topic, event.eventId().toString(), event)
                    .whenComplete((result, error) -> {
                        if (error != null) log.warn("Audit Kafka publish failed eventId={} error={}",
                                event.eventId(), error.getClass().getSimpleName());
                    });
        } catch (RuntimeException ex) {
            log.warn("Audit Kafka publish rejected eventId={} error={}",
                    event.eventId(), ex.getClass().getSimpleName());
        }
    }
}
