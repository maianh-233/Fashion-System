package com.fashionsystem.fashion_system.realtime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class StompRealtimePublisherTest {

    @Test
    void routesThroughCanonicalPrincipalAndSharedDestination() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        StompRealtimePublisher publisher = new StompRealtimePublisher(template);
        UUID userId = UUID.randomUUID();
        RealtimeEvent<Map<String, String>> event = new RealtimeEvent<>(
                1,
                UUID.randomUUID(),
                "ORDER_UPDATED",
                "ORDER",
                RealtimeAccountType.USER,
                userId,
                Map.of("status", "confirmed"),
                Instant.now());

        publisher.sendToUser(userId, event);

        verify(template).convertAndSendToUser(eq("USER:" + userId), eq("/queue/events"), eq(event));
    }
}
