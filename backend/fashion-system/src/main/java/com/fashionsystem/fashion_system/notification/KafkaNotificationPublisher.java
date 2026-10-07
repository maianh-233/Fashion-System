package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.kafka.enabled", havingValue = "true")
public class KafkaNotificationPublisher implements NotificationPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final String topic;
    private final Clock clock;

    public KafkaNotificationPublisher(
            @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, NotificationEvent> kafkaTemplate,
            @Value("${notification.kafka.topic:fashion.notifications}") String topic,
            @Qualifier("notificationClock") Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.clock = clock;
    }

    @Override
    public void publish(NotificationRequest request) {
        NotificationEvent event = toEvent(request);
        try {
            kafkaTemplate.send(topic, event.eventId().toString(), event)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Notification Kafka publish failed for event {}", event.eventId(), failure);
                        }
                    });
        } catch (RuntimeException failure) {
            log.error("Notification Kafka publish failed synchronously for event {}", event.eventId(), failure);
        }
    }

    private NotificationEvent toEvent(NotificationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        List<RealtimeIdentity> recipients = request.recipientIdentities() == null
                ? List.of()
                : new LinkedHashSet<>(request.recipientIdentities()).stream()
                        .filter(Objects::nonNull)
                        .toList();
        if (recipients.isEmpty() || blank(request.type()) || blank(request.title()) || blank(request.message())) {
            throw new IllegalArgumentException("Notification requires recipients, type, title, and message");
        }
        return new NotificationEvent(
                1,
                UUID.randomUUID(),
                recipients,
                request.actorUserId(),
                request.type().trim(),
                request.title().trim(),
                request.message().trim(),
                request.entityType(),
                request.entityId(),
                request.actionUrl(),
                request.metadata(),
                clock.instant());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
