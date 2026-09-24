package com.fashionsystem.fashion_system.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditEventPublisher;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolProperties;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolReader;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolWriter;
import com.fashionsystem.fashion_system.entity.AuditOutbox;
import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import tools.jackson.databind.ObjectMapper;

class AuditOutboxDeliveryServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-24T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));
    private final ObjectMapper mapper = new ObjectMapper();
    @TempDir Path directory;

    @Test
    void broker_failure_keeps_file_marked_and_schedules_retry() {
        AuditOutbox row = pending();
        AuditEventPublisher publisher = event -> CompletableFuture.failedFuture(new IllegalStateException("broker down\nsecret"));
        var service = service(publisher);

        service.deliver(row);

        assertThat(row.getFileAppendedAt()).isEqualTo(NOW);
        assertThat(row.getKafkaPublishedAt()).isNull();
        assertThat(row.getAttemptCount()).isEqualTo(1);
        assertThat(row.getNextAttemptAt()).isAfter(NOW);
        assertThat(row.getLastError()).doesNotContain("\n", "secret");
        assertThat(new AuditSpoolReader(properties()).readAndVerify(LocalDate.of(2026, 9, 24)).events()).hasSize(1);
    }

    @Test
    void retry_after_file_mark_does_not_append_again_and_publishes_kafka() {
        AuditOutbox row = pending();
        var first = service(event -> CompletableFuture.failedFuture(new IllegalStateException("offline")));
        first.deliver(row);
        AuditEventPublisher successful = event -> CompletableFuture.completedFuture(null);

        service(successful).deliver(row);

        assertThat(row.getKafkaPublishedAt()).isEqualTo(NOW);
        assertThat(row.getNextAttemptAt()).isNull();
        assertThat(new AuditSpoolReader(properties()).readAndVerify(LocalDate.of(2026, 9, 24)).events()).hasSize(1);
    }

    @Test
    void kafka_is_marked_only_after_acknowledgement() throws InterruptedException {
        AuditOutbox row = pending();
        CompletableFuture<Void> ack = new CompletableFuture<>();
        CountDownLatch invoked = new CountDownLatch(1);
        var service = service(event -> { invoked.countDown(); return ack; });
        CompletableFuture<Void> processing = CompletableFuture.runAsync(() -> service.deliver(row));
        assertThat(invoked.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(row.getKafkaPublishedAt()).isNull();
        ack.complete(null);
        processing.join();
        assertThat(row.getKafkaPublishedAt()).isEqualTo(NOW);
    }

    @Test
    void dispatcher_uses_bounded_pending_batch_and_continues_after_a_failed_row() {
        AuditOutboxRepository repository = mock(AuditOutboxRepository.class);
        AuditOutboxDeliveryService service = mock(AuditOutboxDeliveryService.class);
        AuditOutbox first = pending();
        AuditOutbox second = pending();
        when(repository.lockPending(NOW, 25)).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("bad row")).when(service).deliver(first);

        new AuditOutboxDispatcher(repository, service, clock, 25).dispatch();

        verify(repository).lockPending(NOW, 25);
        verify(service).deliver(second);
    }

    private AuditOutboxDeliveryService service(AuditEventPublisher publisher) {
        return new AuditOutboxDeliveryService(new AuditSpoolWriter(properties(), clock),
                publisher, mapper, clock);
    }

    private AuditSpoolProperties properties() {
        return new AuditSpoolProperties(directory, "test", ZoneId.of("UTC"));
    }

    private AuditOutbox pending() {
        return AuditOutbox.pending(AuditEvent.builder().eventId(UUID.randomUUID())
                .action("CREATE").occurredAt(NOW).build(), mapper);
    }

}
