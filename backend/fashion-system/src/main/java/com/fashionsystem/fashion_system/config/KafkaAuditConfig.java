package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolProperties;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolReader;
import com.fashionsystem.fashion_system.audit.spool.AuditSpoolWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableScheduling
public class KafkaAuditConfig {
    @Bean
    Clock auditClock() {
        return Clock.systemUTC();
    }

    @Bean
    AuditSpoolProperties auditSpoolProperties(org.springframework.core.env.Environment env) {
        return new AuditSpoolProperties(
                Path.of(env.getProperty("audit.spool.directory", "./var/audit-spool")),
                env.getProperty("audit.spool.instance-id", "local"),
                ZoneId.of(env.getProperty("audit.spool.zone-id", "Asia/Saigon")));
    }

    @Bean
    AuditSpoolWriter auditSpoolWriter(AuditSpoolProperties properties, Clock auditClock) {
        return new AuditSpoolWriter(properties, auditClock);
    }

    @Bean
    AuditSpoolReader auditSpoolReader(AuditSpoolProperties properties) {
        return new AuditSpoolReader(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    ProducerFactory<String, AuditEvent> auditProducerFactory(org.springframework.core.env.Environment env) {
        return new DefaultKafkaProducerFactory<>(
                KafkaClientConfigSupport.producerProperties(env, "audit.kafka"));
    }
    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    KafkaTemplate<String, AuditEvent> auditKafkaTemplate(ProducerFactory<String, AuditEvent> factory) {
        return new KafkaTemplate<>(factory);
    }
    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    ConsumerFactory<String, AuditEvent> auditConsumerFactory(org.springframework.core.env.Environment env) {
        return new DefaultKafkaConsumerFactory<>(KafkaClientConfigSupport.consumerProperties(
                env, "audit.kafka", "fashion-audit-consumer", AuditEvent.class));
    }
    @Bean
    @ConditionalOnProperty(name = "audit.kafka.enabled", havingValue = "true")
    ConcurrentKafkaListenerContainerFactory<String, AuditEvent> auditKafkaListenerContainerFactory(
            ConsumerFactory<String, AuditEvent> consumerFactory, KafkaTemplate<String, AuditEvent> template,
            org.springframework.core.env.Environment env) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, AuditEvent>();
        f.setConsumerFactory(consumerFactory);
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        f.setCommonErrorHandler(KafkaClientConfigSupport.errorHandler(
                template,
                env.getProperty("audit.kafka.retry-backoff-ms", Long.class, 1000L),
                env.getProperty("audit.kafka.retry-attempts", Long.class, 2L)));
        return f;
    }
}
