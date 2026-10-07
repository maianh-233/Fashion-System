package com.fashionsystem.fashion_system.realtime;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;

/** Shared boundary used by features; callers never access WebSocket sessions directly. */
public interface RealtimePublisher {

    void sendToIdentity(RealtimeIdentity recipient, RealtimeEvent<?> event);

    default void sendToUser(UUID userId, RealtimeEvent<?> event) {
        sendToIdentity(RealtimeIdentity.user(userId), event);
    }

    default void sendToUsers(Collection<UUID> userIds, RealtimeEvent<?> event) {
        if (userIds == null) {
            return;
        }
        new LinkedHashSet<>(userIds).stream()
                .filter(Objects::nonNull)
                .forEach(userId -> sendToUser(userId, event));
    }
}
