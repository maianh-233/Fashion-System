package com.fashionsystem.fashion_system.audit.reconcile;

import com.fashionsystem.fashion_system.FashionSystemApplication;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolProperties;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolReader;
import com.fashionsystem.fashion_system.repository.AuditLogRepository;
import com.fashionsystem.fashion_system.repository.AuditOutboxRepository;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** One-shot operator entry point. Never prints event payloads or spool lines. */
public final class AuditReconciliationRunner {
    private static final String DATE_OPTION = "--audit.reconcile-date=";

    private AuditReconciliationRunner() {}

    public static void main(String[] args) {
        if (args.length != 1 || !args[0].matches("--audit\\.reconcile-date=\\d{4}-\\d{2}-\\d{2}")) {
            System.err.println("Usage: --audit.reconcile-date=YYYY-MM-DD");
            System.exit(2);
        }

        final LocalDate date;
        try {
            date = LocalDate.parse(args[0].substring(DATE_OPTION.length()));
        } catch (DateTimeParseException error) {
            System.err.println("Invalid reconciliation date; expected a real YYYY-MM-DD date");
            System.exit(2);
            return;
        }

        try (var context = new SpringApplicationBuilder(FashionSystemApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off", "spring.task.scheduling.enabled=false",
                        "audit.reconcile.manual=true")
                .run(args)) {
            var properties = context.getBean(AuditSpoolProperties.class);
            var reader = context.getBean(AuditSpoolReader.class);
            var auditLogs = context.getBean(AuditLogRepository.class);
            var outbox = context.getBean(AuditOutboxRepository.class);
            var reconciliation = context.getBean(AuditReconciliationService.class);
            List<Path> files = spoolFiles(properties.directory(), date);
            if (files.isEmpty()) throw new IllegalStateException("No audit spool files for " + date);

            Set<UUID> eventIds = new java.util.HashSet<>();
            for (Path file : files) {
                var result = reader.readAndVerify(file);
                if (!result.valid()) throw new IllegalStateException("Invalid audit spool hash chain: " + file.getFileName());
                result.events().forEach(event -> eventIds.add(event.eventId()));
            }

            long pending = outbox.countByFileAppendedAtIsNullOrKafkaPublishedAtIsNull();
            if (pending > 0) {
                System.out.printf("files=%d events=%d pending_outbox=%d%n", files.size(), eventIds.size(), pending);
                throw new IllegalStateException("Pending audit outbox records; retry after delivery");
            }

            long missing = countMissing(eventIds, auditLogs);
            reconciliation.reconcile(date);
            System.out.printf("files=%d events=%d missing_before_reconcile=%d pending_outbox=0%n",
                    files.size(), eventIds.size(), missing);
            if (missing > 0) throw new IllegalStateException("Missing audit records were republished; verify consumption and rerun");
        } catch (Exception error) {
            System.err.println("Audit reconciliation failed: " + error.getMessage());
            System.exit(1);
        }
    }

    private static long countMissing(Set<UUID> ids, AuditLogRepository auditLogs) {
        List<UUID> ordered = new ArrayList<>(ids);
        long missing = 0;
        for (int start = 0; start < ordered.size(); start += 500) {
            var batch = ordered.subList(start, Math.min(ordered.size(), start + 500));
            missing += batch.size() - Set.copyOf(auditLogs.findExistingEventIds(batch)).size();
        }
        return missing;
    }

    private static List<Path> spoolFiles(Path directory, LocalDate date) {
        if (!Files.isDirectory(directory)) return List.of();
        List<Path> files = new ArrayList<>();
        try (var paths = Files.newDirectoryStream(directory, "business-audit-" + date + "-*.jsonl")) {
            for (Path path : paths) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) files.add(path);
            }
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not list audit spools", error);
        }
        files.sort(Path::compareTo);
        return files;
    }
}
