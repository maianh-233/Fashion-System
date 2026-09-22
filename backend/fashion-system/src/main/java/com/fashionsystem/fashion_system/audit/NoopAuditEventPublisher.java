package com.fashionsystem.fashion_system.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoopAuditEventPublisher implements AuditEventPublisher {
    @Override public void publish(AuditEvent event) {}
}
