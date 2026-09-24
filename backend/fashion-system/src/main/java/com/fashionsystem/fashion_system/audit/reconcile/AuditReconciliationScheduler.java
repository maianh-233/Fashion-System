package com.fashionsystem.fashion_system.audit.reconcile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuditReconciliationScheduler {
    private static final ZoneId ZONE = ZoneId.of("Asia/Saigon");
    private final AuditReconciliationService reconciliation;
    private final Clock clock;

    public AuditReconciliationScheduler(AuditReconciliationService reconciliation, Clock clock) {
        this.reconciliation = reconciliation;
        this.clock = clock;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Saigon")
    public void reconcilePreviousDay() {
        reconciliation.reconcileOutstandingThrough(LocalDate.now(clock.withZone(ZONE)).minusDays(1));
    }
}
