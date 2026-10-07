package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;

public record PersistedNotification(RealtimeIdentity recipient, NotificationResponse notification) {
}
