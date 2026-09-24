package com.fashionsystem.fashion_system.audit.spool;

import com.fashionsystem.fashion_system.audit.*;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class AuditSpoolWriterTest {
    @TempDir Path dir;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-23T17:30:00Z"), ZoneOffset.UTC);

    @Test void appendsTwoEventsInOneVerifiableChain() throws Exception {
        var writer = new AuditSpoolWriter(new AuditSpoolProperties(dir, "node-a", ZoneId.of("Asia/Saigon")), CLOCK);
        var first = event("EMPLOYEE_UPDATE");
        var second = event("EMPLOYEE_DELETE");
        Path file = writer.append(first);
        assertThat(writer.append(second)).isEqualTo(file);
        assertThat(file.getFileName().toString()).isEqualTo("business-audit-2026-09-24-node-a.jsonl");
        assertThat(Files.readAllLines(file)).hasSize(2);
        var result = new AuditSpoolReader(new AuditSpoolProperties(dir, "node-a", ZoneId.of("Asia/Saigon"))).readAndVerify(LocalDate.of(2026, 9, 24));
        assertThat(result.valid()).isTrue();
        assertThat(result.events()).extracting(AuditEvent::eventId).containsExactly(first.eventId(), second.eventId());
    }

    @Test void instanceIdsUseDifferentPaths() {
        var date = LocalDate.of(2026, 9, 24);
        assertThat(new AuditSpoolWriter(new AuditSpoolProperties(dir, "node-a", ZoneId.of("Asia/Saigon")), CLOCK).pathFor(date))
                .isNotEqualTo(new AuditSpoolWriter(new AuditSpoolProperties(dir, "node-b", ZoneId.of("Asia/Saigon")), CLOCK).pathFor(date));
    }

    static AuditEvent event(String action) {
        return new AuditEvent(UUID.randomUUID(), 1, AuditActorType.SYSTEM, null, "SYSTEM", action,
                null, null, null, "job", null, null, 0, List.of(), Instant.parse("2026-09-23T17:30:00Z"));
    }
}
