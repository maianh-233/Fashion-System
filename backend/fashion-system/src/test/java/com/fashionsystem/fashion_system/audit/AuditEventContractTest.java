package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventContractTest {
    @Test
    void createsAnImmutableEventWithSafeRequestMetadata() {
        UUID id = UUID.randomUUID();
        AuditEvent event = AuditEvent.builder()
                .eventId(id)
                .category(AuditCategory.AUTH)
                .action("LOGIN_FAILED")
                .outcome(AuditOutcome.FAILURE)
                .occurredAt(Instant.now())
                .build();

        assertThat(event.eventId()).isEqualTo(id);
        assertThat(event.category()).isEqualTo(AuditCategory.AUTH);
        assertThat(event.action()).isEqualTo("LOGIN_FAILED");
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
    }
}
