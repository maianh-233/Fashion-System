package com.fashionsystem.fashion_system.audit;

public interface AuditEventPublisher {
    void publish(AuditEvent event);
}
