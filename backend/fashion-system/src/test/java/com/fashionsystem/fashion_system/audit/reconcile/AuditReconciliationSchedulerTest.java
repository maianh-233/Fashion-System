package com.fashionsystem.fashion_system.audit.reconcile;

import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuditReconciliationSchedulerTest {
    @Test
    void usesPreviousSaigonDayAtUtcBoundary() {
        AuditReconciliationService service = mock(AuditReconciliationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-23T18:00:00Z"), ZoneOffset.UTC);

        new AuditReconciliationScheduler(service, clock).reconcilePreviousDay();

        verify(service).reconcileOutstandingThrough(LocalDate.of(2026, 9, 23));
    }
}
