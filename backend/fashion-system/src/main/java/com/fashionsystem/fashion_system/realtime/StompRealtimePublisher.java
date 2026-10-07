package com.fashionsystem.fashion_system.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Single-instance STOMP implementation; replaceable by a distributed fan-out publisher later. */
@Component
@RequiredArgsConstructor
public class StompRealtimePublisher implements RealtimePublisher {

    public static final String USER_EVENTS_DESTINATION = "/queue/events";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToIdentity(RealtimeIdentity recipient, RealtimeEvent<?> event) {
        messagingTemplate.convertAndSendToUser(
                recipient.canonicalPrincipal(),
                USER_EVENTS_DESTINATION,
                event.addressedTo(recipient));
    }
}
