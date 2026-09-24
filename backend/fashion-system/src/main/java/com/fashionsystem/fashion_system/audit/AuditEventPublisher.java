package com.fashionsystem.fashion_system.audit;

import java.util.concurrent.CompletionStage;

public interface AuditEventPublisher {
    CompletionStage<Void> publish(AuditEvent event);
}
