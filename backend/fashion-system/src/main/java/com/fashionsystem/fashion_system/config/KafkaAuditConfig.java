package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolProperties;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@EnableScheduling
public class KafkaAuditConfig {
    @Bean
    Clock auditClock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditSpoolWriter auditSpoolWriter(org.springframework.core.env.Environment env, Clock auditClock) {
        return new AuditSpoolWriter(new AuditSpoolProperties(
                Path.of(env.getProperty("audit.spool.directory", "./var/audit-spool")),
                env.getProperty("audit.spool.instance-id", "local"),
                ZoneId.of(env.getProperty("audit.spool.zone-id", "Asia/Saigon"))), auditClock);
    }

    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    ProducerFactory<String, AuditEvent> auditProducerFactory(org.springframework.core.env.Environment env) {
        Map<String, Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("audit.kafka.bootstrap-servers", "localhost:9092"));
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        p.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(p);
    }
    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    KafkaTemplate<String, AuditEvent> auditKafkaTemplate(ProducerFactory<String, AuditEvent> factory) {
        return new KafkaTemplate<>(factory);
    }
    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    ConsumerFactory<String, AuditEvent> auditConsumerFactory(org.springframework.core.env.Environment env) {
        Map<String, Object> p = new HashMap<>();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("audit.kafka.bootstrap-servers", "localhost:9092"));
        p.put(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("audit.kafka.consumer-group", "fashion-audit-consumer"));
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        p.put(JsonDeserializer.TRUSTED_PACKAGES, "com.fashionsystem.fashion_system.audit");
        p.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AuditEvent.class.getName());
        p.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(p);
    }
    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    ConcurrentKafkaListenerContainerFactory<String, AuditEvent> auditKafkaListenerContainerFactory(
            ConsumerFactory<String, AuditEvent> consumerFactory, KafkaTemplate<String, AuditEvent> template,
            org.springframework.core.env.Environment env) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, AuditEvent>();
        f.setConsumerFactory(consumerFactory);
        f.setCommonErrorHandler(new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template),
                new FixedBackOff(env.getProperty("audit.kafka.retry-backoff-ms", Long.class, 1000L),
                        env.getProperty("audit.kafka.retry-attempts", Long.class, 2L))));
        return f;
    }
}
