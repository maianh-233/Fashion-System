package com.fashionsystem.fashion_system.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Keeps business services independent from recipient resolution, Kafka, and realtime details. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationIntentPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(NotificationIntent intent) {
        try {
            applicationEventPublisher.publishEvent(intent);
        } catch (RuntimeException failure) {
            log.warn("Unable to register notification intent for {}", intent.entityId(), failure);
        }
    }
}
