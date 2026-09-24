package com.fashionsystem.fashion_system.audit.reconcile;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.AuditEventPublisher;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolProperties;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolReader;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuditReconciliationService {
    private static final int BATCH_SIZE = 500;
    private final AuditSpoolProperties properties;
    private final AuditSpoolReader reader;
    private final AuditLogRepository auditLogs;
    private final AuditOutboxRepository outbox;
    private final AuditEventPublisher publisher;
    private final Set<LocalDate> previouslyVerified = ConcurrentHashMap.newKeySet();

    public AuditReconciliationService(AuditSpoolProperties properties, AuditSpoolReader reader,
            AuditLogRepository auditLogs, AuditOutboxRepository outbox, AuditEventPublisher publisher) {
        this.properties = properties;
        this.reader = reader;
        this.auditLogs = auditLogs;
        this.outbox = outbox;
        this.publisher = publisher;
    }

    /** Revisit retained older files, including yesterday's file, on every scheduled run. */
    public void reconcileOutstandingThrough(LocalDate lastDay) {
        Set<LocalDate> dates = new TreeSet<>();
        for (Path path : filesMatching("business-audit-*.jsonl")) {
            String name = path.getFileName().toString();
            if (name.length() < 27 || !name.startsWith("business-audit-")) continue;
            try {
                LocalDate date = LocalDate.parse(name.substring(15, 25));
                if (!date.isAfter(lastDay)) dates.add(date);
            } catch (RuntimeException ignored) {
                // An unrelated filename is not an audit spool for a calendar day.
            }
        }
        dates.add(lastDay);
        dates.forEach(this::reconcile);
    }

    public void reconcile(LocalDate date) {
        List<Path> files = filesMatching("business-audit-" + date + "-*.jsonl");
        if (files.isEmpty()) {
            previouslyVerified.remove(date);
            return;
        }

        LinkedHashMap<UUID, AuditEvent> events = new LinkedHashMap<>();
        for (Path file : files) {
            var result = reader.readAndVerify(file);
            if (!result.valid()) {
                previouslyVerified.remove(date);
                return;
            }
            for (AuditEvent event : result.events()) events.putIfAbsent(event.eventId(), event);
        }

        if (outbox.countByFileAppendedAtIsNullOrKafkaPublishedAtIsNull() > 0) {
            previouslyVerified.remove(date);
            return;
        }

        List<UUID> ids = new ArrayList<>(events.keySet());
        boolean missing = false;
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            Collection<UUID> batch = ids.subList(start, Math.min(ids.size(), start + BATCH_SIZE));
            Set<UUID> present = Set.copyOf(auditLogs.findExistingEventIds(batch));
            for (UUID id : batch) {
                if (!present.contains(id)) {
                    missing = true;
                    publisher.publish(events.get(id)).toCompletableFuture().join();
                }
            }
        }
        if (missing) {
            previouslyVerified.remove(date);
            return;
        }
        if (previouslyVerified.add(date)) return;
        for (Path file : files) {
            try {
                Files.delete(file);
            } catch (IOException error) {
                throw new IllegalStateException("Could not delete verified audit spool " + file, error);
            }
        }
        previouslyVerified.remove(date);
    }

    private List<Path> filesMatching(String glob) {
        if (!Files.isDirectory(properties.directory())) return List.of();
        List<Path> files = new ArrayList<>();
        try (var paths = Files.newDirectoryStream(properties.directory(), glob)) {
            for (Path path : paths) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) files.add(path);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Could not list audit spools", error);
        }
        files.sort(Path::compareTo);
        return files;
    }
}
