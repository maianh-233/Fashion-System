package com.fashionsystem.fashion_system.audit.reconcile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.audit.AuditActorType;
import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditEventPublisher;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolProperties;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolReader;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolWriter;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditReconciliationServiceTest {
    @TempDir Path directory;
    private final LocalDate date = LocalDate.of(2026, 9, 24);
    private final ZoneId zone = ZoneId.of("Asia/Saigon");
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T01:00:00Z"), ZoneOffset.UTC);
    private final AuditLogRepository logs = mock(AuditLogRepository.class);
    private final AuditOutboxRepository outbox = mock(AuditOutboxRepository.class);
    private final AuditEventPublisher publisher = mock(AuditEventPublisher.class);

    @Test
    void missingEventIsRepublishedAndFileKept() throws Exception {
        AuditEvent event = event();
        Path file = writer("node-a").append(event);
        when(logs.findExistingEventIds(any())).thenReturn(List.of());
        when(publisher.publish(event)).thenReturn(CompletableFuture.completedFuture(null));

        service().reconcile(date);

        verify(publisher).publish(event);
        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void invalidHashIsKeptWithoutRepublishing() throws Exception {
        Path file = writer("node-a").append(event());
        Files.writeString(file, Files.readString(file).replace("JOB_UPDATE", "JOB_DELETE"));

        service().reconcile(date);

        assertThat(Files.exists(file)).isTrue();
        verifyNoInteractions(publisher, logs);
    }

    @Test
    void allInstanceFilesAreDeletedOnlyAfterLaterCompleteVerification() throws Exception {
        AuditEvent first = event();
        AuditEvent second = event();
        Path firstFile = writer("node-a").append(first);
        Path secondFile = writer("node-b").append(second);
        when(logs.findExistingEventIds(any())).thenReturn(List.of(first.eventId(), second.eventId()));
        AuditReconciliationService service = service();

        service.reconcile(date);
        assertThat(Files.exists(firstFile)).isTrue();
        assertThat(Files.exists(secondFile)).isTrue();
        service.reconcile(date);

        assertThat(Files.exists(firstFile)).isFalse();
        assertThat(Files.exists(secondFile)).isFalse();
        verifyNoInteractions(publisher);
    }

    @Test
    void unfinishedOutboxKeepsCompleteFile() throws Exception {
        Path file = writer("node-a").append(event());
        when(outbox.countByFileAppendedAtIsNullOrKafkaPublishedAtIsNull()).thenReturn(1L);

        service().reconcile(date);

        assertThat(Files.exists(file)).isTrue();
        verifyNoInteractions(logs, publisher);
    }

    private AuditSpoolWriter writer(String instanceId) {
        return new AuditSpoolWriter(new AuditSpoolProperties(directory, instanceId, zone), clock);
    }

    private AuditReconciliationService service() {
        var properties = new AuditSpoolProperties(directory, "local", zone);
        return new AuditReconciliationService(properties, new AuditSpoolReader(properties), logs, outbox, publisher);
    }

    private AuditEvent event() {
        return new AuditEvent(UUID.randomUUID(), 1, AuditActorType.SYSTEM, null, "SYSTEM", "JOB_UPDATE",
                null, null, null, "nightly-job", null, null, 0, List.of(), clock.instant());
    }
}
