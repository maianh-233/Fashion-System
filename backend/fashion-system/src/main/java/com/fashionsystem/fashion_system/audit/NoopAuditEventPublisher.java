package com.fashionsystem.fashion_system.audit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoopAuditEventPublisher implements AuditEventPublisher {
    @Override public CompletionStage<Void> publish(AuditEvent event) {
        return CompletableFuture.failedFuture(new IllegalStateException("Audit Kafka publishing is disabled"));
    }
}
