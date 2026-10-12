package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The enabled Kafka profile must coexist with the audit beans that inject an unqualified Clock. */
@SpringBootTest(properties = {
        "notification.kafka.enabled=true",
        "notification.kafka.topic=fashion.notifications.context-test",
        "notification.kafka.consumer-group=fashion-notification-context-test"
})
class NotificationKafkaContextTest {

    @Autowired NotificationPublisher publisher;
    @Autowired NotificationConsumer consumer;

    @Test
    void wiresKafkaNotificationPipelineAlongsideAudit() {
        assertThat(publisher).isInstanceOf(KafkaNotificationPublisher.class);
        assertThat(consumer).isNotNull();
    }
}
