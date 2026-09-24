package com.fashionsystem.fashion_system.audit.spool;

import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class AuditSpoolReaderTest {
    @TempDir Path dir;
    private final LocalDate date = LocalDate.of(2026, 9, 24);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T01:00:00Z"), ZoneOffset.UTC);

    @Test void detectsTamperedMiddleLineWithoutChangingFile() throws Exception {
        var properties = new AuditSpoolProperties(dir, "node-a", ZoneId.of("Asia/Saigon"));
        var writer = new AuditSpoolWriter(properties, clock);
        Path file = writer.append(AuditSpoolWriterTest.event("EMPLOYEE_UPDATE"));
        writer.append(AuditSpoolWriterTest.event("EMPLOYEE_DELETE"));
        String altered = Files.readString(file).replace("EMPLOYEE_UPDATE", "EMPLOYEE_CREATE");
        Files.writeString(file, altered);
        assertThat(new AuditSpoolReader(properties).readAndVerify(date).valid()).isFalse();
        assertThat(Files.readString(file)).isEqualTo(altered);
    }

    @Test void reportsDuplicateIdsSeparately() throws Exception {
        var properties = new AuditSpoolProperties(dir, "node-a", ZoneId.of("Asia/Saigon"));
        var writer = new AuditSpoolWriter(properties, clock);
        var event = AuditSpoolWriterTest.event("EMPLOYEE_UPDATE");
        writer.append(event);
        writer.append(event);
        var result = new AuditSpoolReader(properties).readAndVerify(date);
        assertThat(result.valid()).isTrue();
        assertThat(result.duplicateEventIds()).containsExactly(event.eventId());
    }
}
