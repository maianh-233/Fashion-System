package com.fashionsystem.fashion_system.audit.spool;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

public final class AuditSpoolReader {
    private final AuditSpoolProperties properties;

    public AuditSpoolReader(AuditSpoolProperties properties) { this.properties = properties; }

    public AuditSpoolReadResult readAndVerify(LocalDate date) {
        var path = properties.directory().resolve("business-audit-" + date + "-" + properties.instanceId() + ".jsonl");
        return readAndVerify(path);
    }

    public AuditSpoolReadResult readAndVerify(java.nio.file.Path path) {
        if (!Files.exists(path)) return new AuditSpoolReadResult(true, List.of(), Set.of());
        var events = new ArrayList<AuditEvent>();
        var seen = new HashSet<UUID>();
        var duplicates = new LinkedHashSet<UUID>();
        String previous = AuditSpoolLine.GENESIS;
        boolean valid = true;
        try (var lines = Files.lines(path)) {
            for (String text : (Iterable<String>) lines::iterator) {
                try {
                    AuditSpoolLine line = AuditSpoolLine.MAPPER.readValue(text, AuditSpoolLine.class);
                    AuditEvent event = AuditSpoolLine.MAPPER.treeToValue(line.payload(), AuditEvent.class);
                    String payloadHash = AuditSpoolLine.sha256(AuditSpoolLine.canonical(line.payload()));
                    if (!previous.equals(line.previousHash()) || !payloadHash.equals(line.payloadHash())
                            || !AuditSpoolLine.sha256(previous + payloadHash + line.eventId()).equals(line.lineHash())
                            || !event.eventId().equals(line.eventId())) valid = false;
                    if (!seen.add(line.eventId())) duplicates.add(line.eventId());
                    events.add(event);
                    previous = line.lineHash();
                } catch (RuntimeException e) {
                    valid = false;
                }
            }
        } catch (IOException | UncheckedIOException e) {
            throw new IllegalStateException("Could not read audit spool " + path, e);
        }
        return new AuditSpoolReadResult(valid, List.copyOf(events), Set.copyOf(duplicates));
    }
}
