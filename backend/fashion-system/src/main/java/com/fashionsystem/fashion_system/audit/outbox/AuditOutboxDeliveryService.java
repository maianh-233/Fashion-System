package com.fashionsystem.fashion_system.audit.outbox;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditEventPublisher;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolWriter;
import com.fashionsystem.fashion_system.entity.AuditOutbox;
import java.time.Clock;
import java.util.concurrent.CompletionException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditOutboxDeliveryService {
    private final AuditSpoolWriter spoolWriter;
    private final AuditEventPublisher publisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditOutboxDeliveryService(AuditSpoolWriter spoolWriter, AuditEventPublisher publisher,
                                      ObjectMapper objectMapper, Clock clock) {
        this.spoolWriter = spoolWriter;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void deliver(AuditOutbox row) {
        try {
            AuditEvent event = objectMapper.treeToValue(row.getPayload(), AuditEvent.class);
            if (row.getFileAppendedAt() == null) {
                spoolWriter.append(event);
                row.markFileAppended(clock.instant());
            }
            if (row.getKafkaPublishedAt() == null) {
                publisher.publish(event).toCompletableFuture().join();
                row.markKafkaPublished(clock.instant());
            }
        } catch (RuntimeException error) {
            Throwable cause = error instanceof CompletionException && error.getCause() != null
                    ? error.getCause() : error;
            row.recordFailure(clock.instant(), cause.getClass().getSimpleName());
        }
    }
}
