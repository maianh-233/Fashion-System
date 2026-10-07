package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Kafka contract for asynchronous persistent notifications. */
public record NotificationEvent(
        int schemaVersion,
        UUID eventId,
        List<RealtimeIdentity> recipientIdentities,
        UUID actorUserId,
        String type,
        String title,
        String message,
        String entityType,
        UUID entityId,
        String actionUrl,
        Map<String, Object> metadata,
        Instant occurredAt) {

    public NotificationEvent {
        recipientIdentities = recipientIdentities == null ? List.of() : List.copyOf(recipientIdentities);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
