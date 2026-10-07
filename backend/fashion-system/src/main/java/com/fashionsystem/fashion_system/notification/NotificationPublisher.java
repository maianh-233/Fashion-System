package com.fashionsystem.fashion_system.notification;

/** Non-critical asynchronous notification boundary used by business workflows. */
public interface NotificationPublisher {
    void publish(NotificationRequest request);
}
