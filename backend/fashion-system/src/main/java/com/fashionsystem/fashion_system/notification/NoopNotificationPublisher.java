package com.fashionsystem.fashion_system.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoopNotificationPublisher implements NotificationPublisher {
    @Override
    public void publish(NotificationRequest request) {
        log.warn("Notification Kafka publishing is disabled; notification was not queued");
    }
}
