package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.realtime.RealtimeEvent;
import com.fashionsystem.fashion_system.realtime.RealtimePublisher;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.kafka.enabled", havingValue = "true")
public class NotificationConsumer {

    private final NotificationPersistenceService persistenceService;
    private final RealtimePublisher realtimePublisher;

    @KafkaListener(
            topics = "${notification.kafka.topic:fashion.notifications}",
            containerFactory = "notificationKafkaListenerContainerFactory")
    public void consume(NotificationEvent event, Acknowledgment acknowledgment) {
        var persistedNotifications = persistenceService.persist(event);
        for (PersistedNotification persisted : persistedNotifications) {
            try {
                var notification = persisted.notification();
                realtimePublisher.sendToIdentity(
                        persisted.recipient(),
                        new RealtimeEvent<>(
                                1,
                                UUID.randomUUID(),
                                "NOTIFICATION_CREATED",
                                "NOTIFICATION",
                                persisted.recipient().accountType(),
                                persisted.recipient().accountId(),
                                notification,
                                Instant.now()));
            } catch (RuntimeException failure) {
                log.warn(
                        "Realtime notification delivery failed for persisted notification {}",
                        persisted.notification().id(),
                        failure);
            }
        }
        acknowledgment.acknowledge();
    }
}
