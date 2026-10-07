package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.entity.Notification;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/** Public notification history/realtime payload. Legacy delivery fields stay private. */
public record NotificationResponse(
        UUID id,
        UUID eventId,
        String type,
        String title,
        String message,
        String entityType,
        UUID entityId,
        String actionUrl,
        Map<String, Object> metadata,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getActionUrl(),
                notification.getMetadata(),
                notification.getCreatedAt() == null
                        ? null
                        : notification.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}
