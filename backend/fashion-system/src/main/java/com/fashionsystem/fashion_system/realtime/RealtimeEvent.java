package com.fashionsystem.fashion_system.realtime;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Versioned, feature-neutral envelope delivered over the shared realtime connection. */
public record RealtimeEvent<T>(
        int version,
        UUID eventId,
        String eventType,
        String channel,
        RealtimeAccountType recipientType,
        UUID recipientId,
        T payload,
        Instant timestamp) {

    public RealtimeEvent {
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(recipientType, "recipientType must not be null");
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public static <T> RealtimeEvent<T> create(
            String eventType, String channel, RealtimeIdentity recipient, T payload) {
        return new RealtimeEvent<>(
                1,
                UUID.randomUUID(),
                eventType,
                channel,
                recipient.accountType(),
                recipient.accountId(),
                payload,
                Instant.now());
    }

    public RealtimeEvent<T> addressedTo(RealtimeIdentity recipient) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        if (recipientType == recipient.accountType() && recipientId.equals(recipient.accountId())) {
            return this;
        }
        return new RealtimeEvent<>(
                version,
                eventId,
                eventType,
                channel,
                recipient.accountType(),
                recipient.accountId(),
                payload,
                timestamp);
    }
}
