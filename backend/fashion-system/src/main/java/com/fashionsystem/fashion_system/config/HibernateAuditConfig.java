package com.fashionsystem.fashion_system.config;

import com.fashionsystem.fashion_system.audit.HibernateAuditInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class HibernateAuditConfig {
    @Bean
    HibernateAuditInterceptor hibernateAuditInterceptor(ObjectMapper mapper) {
        return new HibernateAuditInterceptor(mapper);
    }

    @Bean
    HibernatePropertiesCustomizer auditInterceptorCustomizer(HibernateAuditInterceptor interceptor) {
        return properties -> properties.put(AvailableSettings.INTERCEPTOR, interceptor);
    }
}
