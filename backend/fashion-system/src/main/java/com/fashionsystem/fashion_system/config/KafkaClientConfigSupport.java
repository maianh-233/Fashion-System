package com.fashionsystem.fashion_system.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

/** Shared Kafka client construction used by audit and notification transports. */
public final class KafkaClientConfigSupport {

    private KafkaClientConfigSupport() {
    }

    public static Map<String, Object> producerProperties(Environment env, String propertyPrefix) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                env.getProperty(propertyPrefix + ".bootstrap-servers", "localhost:9092"));
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        properties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return properties;
    }

    public static Map<String, Object> consumerProperties(
            Environment env, String propertyPrefix, String defaultGroup, Class<?> eventType) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                env.getProperty(propertyPrefix + ".bootstrap-servers", "localhost:9092"));
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                env.getProperty(propertyPrefix + ".consumer-group", defaultGroup));
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, eventType.getPackageName());
        properties.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, eventType.getName());
        properties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return properties;
    }

    public static <T> DefaultErrorHandler errorHandler(
            KafkaTemplate<String, T> template, long retryBackoffMs, long retryAttempts) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(template),
                new FixedBackOff(retryBackoffMs, retryAttempts));
        handler.setCommitRecovered(true);
        return handler;
    }
}
