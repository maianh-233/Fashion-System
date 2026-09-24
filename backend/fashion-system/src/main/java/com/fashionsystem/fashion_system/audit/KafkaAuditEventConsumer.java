package com.fashionsystem.fashion_system.audit;

import com.fashionsystem.fashion_system.service.AuditLogConsumerPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
public class KafkaAuditEventConsumer {
    private final AuditLogConsumerPersistence persistence;
    public KafkaAuditEventConsumer(AuditLogConsumerPersistence persistence) { this.persistence = persistence; }
    @KafkaListener(topics = "${audit.kafka.topic:audit.events}", containerFactory = "auditKafkaListenerContainerFactory")
    public void consume(AuditEvent event, Acknowledgment acknowledgment) {
        persistence.persist(event);
        acknowledgment.acknowledge();
    }
}
