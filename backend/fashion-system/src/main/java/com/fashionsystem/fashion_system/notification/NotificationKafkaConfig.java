package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.config.KafkaClientConfigSupport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@ConditionalOnProperty(name = "notification.kafka.enabled", havingValue = "true")
public class NotificationKafkaConfig {

    @Bean("notificationProducerFactory")
    ProducerFactory<String, NotificationEvent> notificationProducerFactory(Environment env) {
        return new DefaultKafkaProducerFactory<>(
                KafkaClientConfigSupport.producerProperties(env, "notification.kafka"));
    }

    @Bean("notificationKafkaTemplate")
    KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate(
            @Qualifier("notificationProducerFactory") ProducerFactory<String, NotificationEvent> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean("notificationConsumerFactory")
    ConsumerFactory<String, NotificationEvent> notificationConsumerFactory(Environment env) {
        return new DefaultKafkaConsumerFactory<>(KafkaClientConfigSupport.consumerProperties(
                env, "notification.kafka", "fashion-notification-consumer", NotificationEvent.class));
    }

    @Bean("notificationKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> notificationKafkaListenerContainerFactory(
            @Qualifier("notificationConsumerFactory") ConsumerFactory<String, NotificationEvent> consumerFactory,
            @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, NotificationEvent> template,
            Environment env) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, NotificationEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(KafkaClientConfigSupport.errorHandler(
                template,
                env.getProperty("notification.kafka.retry-backoff-ms", Long.class, 1000L),
                env.getProperty("notification.kafka.retry-attempts", Long.class, 2L)));
        return factory;
    }
}
