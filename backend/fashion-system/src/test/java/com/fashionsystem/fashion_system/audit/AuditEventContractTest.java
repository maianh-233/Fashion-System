package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventContractTest {
    @Test
    void createsRequestLevelEventWithActorAndChanges() {
        UUID id = UUID.randomUUID();
        AuditEvent event = new AuditEvent(id, 1, AuditActorType.SYSTEM, null, "SYSTEM",
                "JOB_UPDATE", null, null, null, "nightly-job", null, null,
                0, List.of(), Instant.now());

        assertThat(event.eventId()).isEqualTo(id);
        assertThat(event.actorType()).isEqualTo(AuditActorType.SYSTEM);
        assertThat(event.jobName()).isEqualTo("nightly-job");
        assertThat(event.rowCount()).isZero();
        assertThat(event.changes()).isEmpty();
    }
}
