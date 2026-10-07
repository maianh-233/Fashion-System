package com.fashionsystem.fashion_system.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RealtimeIdentityTest {

    @Test
    void buildsNamespacedCanonicalPrincipals() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        assertThat(RealtimeIdentity.user(userId).canonicalPrincipal()).isEqualTo("USER:" + userId);
        assertThat(RealtimeIdentity.customer(customerId).canonicalPrincipal()).isEqualTo("CUSTOMER:" + customerId);
    }

    @Test
    void envelopeUsesTheActualRecipientIdentity() {
        UUID eventId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();

        RealtimeEvent<Map<String, String>> event = new RealtimeEvent<>(
                1,
                eventId,
                "PROFILE_UPDATED",
                "CUSTOMER",
                RealtimeAccountType.CUSTOMER,
                customerId,
                Map.of("status", "updated"),
                now);

        assertThat(event.recipientType()).isEqualTo(RealtimeAccountType.CUSTOMER);
        assertThat(event.recipientId()).isEqualTo(customerId);
        assertThat(event.eventId()).isNotEqualTo(event.recipientId());
    }
}
