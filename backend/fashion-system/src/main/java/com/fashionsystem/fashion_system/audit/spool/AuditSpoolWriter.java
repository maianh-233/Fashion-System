package com.fashionsystem.fashion_system.audit.spool;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class AuditSpoolWriter {
    private static final ConcurrentHashMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private final AuditSpoolProperties properties;
    private final Clock clock;

    public AuditSpoolWriter(AuditSpoolProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public Path pathFor(LocalDate date) {
        return properties.directory().resolve("business-audit-" + date + "-" + properties.instanceId() + ".jsonl");
    }

    public Path append(AuditEvent event) {
        Path path = pathFor(LocalDate.now(clock.withZone(properties.zoneId()))).toAbsolutePath().normalize();
        ReentrantLock lock = LOCKS.computeIfAbsent(path, ignored -> new ReentrantLock());
        lock.lock();
        try {
            Files.createDirectories(path.getParent());
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
                 var ignored = channel.lock()) {
                String previous = AuditSpoolLine.GENESIS;
                if (channel.size() > 0) {
                    if (channel.size() > Integer.MAX_VALUE) throw new IllegalStateException("Audit spool is too large: " + path);
                    ByteBuffer existing = ByteBuffer.allocate((int) channel.size());
                    channel.position(0);
                    while (existing.hasRemaining() && channel.read(existing) != -1) { }
                    String content = new String(existing.array(), StandardCharsets.UTF_8);
                    if (!content.endsWith("\n")) throw new IllegalStateException("Audit spool has a partial line: " + path);
                    for (String text : content.split("\n")) {
                        AuditSpoolLine line = AuditSpoolLine.MAPPER.readValue(text, AuditSpoolLine.class);
                        String payloadHash = AuditSpoolLine.sha256(AuditSpoolLine.canonical(line.payload()));
                        if (!previous.equals(line.previousHash()) || !payloadHash.equals(line.payloadHash())
                                || !AuditSpoolLine.sha256(previous + payloadHash + line.eventId()).equals(line.lineHash())) {
                            throw new IllegalStateException("Audit spool is damaged: " + path);
                        }
                        previous = line.lineHash();
                    }
                }
                String json = AuditSpoolLine.MAPPER.writeValueAsString(AuditSpoolLine.of(event, previous)) + "\n";
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(json);
                channel.position(channel.size());
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            return path;
        } catch (IOException e) {
            throw new IllegalStateException("Could not append audit spool " + path, e);
        } finally {
            lock.unlock();
        }
    }
}
