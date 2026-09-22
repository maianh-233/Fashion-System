package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.entity.AuditLog;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogConsumerPersistence {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean persist(AuditEvent event) {
        if (repository.existsByEventId(event.eventId())) return false;
        repository.save(AuditLog.builder()
                .eventId(event.eventId())
                .actorUserId(event.actorUserId())
                .username(event.username() == null ? "anonymous" : event.username())
                .action(event.action())
                .entityType(event.entityType() == null ? "SYSTEM" : event.entityType())
                .entityId(event.entityId() == null ? event.eventId() : event.entityId())
                .changedFields(toJson(event))
                .ipAddress(event.ipAddress())
                .userAgent(event.userAgent())
                .createdAt(event.occurredAt() == null ? LocalDateTime.now() : LocalDateTime.ofInstant(event.occurredAt(), java.time.ZoneOffset.UTC))
                .build());
        return true;
    }

    private tools.jackson.databind.JsonNode toJson(AuditEvent event) {
        return new tools.jackson.databind.ObjectMapper().valueToTree(event.metadata());
    }
}
