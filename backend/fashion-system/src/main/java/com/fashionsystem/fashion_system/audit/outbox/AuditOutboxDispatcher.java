package com.fashionsystem.fashion_system.audit.outbox;

import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(AuditOutboxDispatcher.class);
    private final AuditOutboxRepository repository;
    private final AuditOutboxDeliveryService delivery;
    private final Clock clock;
    private final int batchSize;

    public AuditOutboxDispatcher(AuditOutboxRepository repository, AuditOutboxDeliveryService delivery,
                                 Clock clock, @Value("${audit.outbox.batch-size:100}") int batchSize) {
        this.repository = repository;
        this.delivery = delivery;
        this.clock = clock;
        this.batchSize = Math.max(1, Math.min(500, batchSize));
    }

    @Scheduled(fixedDelayString = "${audit.outbox.poll-interval-ms:1000}")
    @Transactional
    public void dispatch() {
        for (var row : repository.lockPending(clock.instant(), batchSize)) {
            try {
                delivery.deliver(row);
            } catch (RuntimeException error) {
                row.recordFailure(clock.instant(), error.getClass().getSimpleName());
                log.warn("Audit outbox delivery failed for eventId={} error={}",
                        row.getEventId(), error.getClass().getSimpleName());
            }
        }
    }
}
