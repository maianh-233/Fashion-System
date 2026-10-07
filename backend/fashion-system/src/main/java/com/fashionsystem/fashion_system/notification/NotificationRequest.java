package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Feature-facing request; callers know recipients and content, never Kafka topic details. */
public record NotificationRequest(
        List<RealtimeIdentity> recipientIdentities,
        UUID actorUserId,
        String type,
        String title,
        String message,
        String entityType,
        UUID entityId,
        String actionUrl,
        Map<String, Object> metadata) {
}
