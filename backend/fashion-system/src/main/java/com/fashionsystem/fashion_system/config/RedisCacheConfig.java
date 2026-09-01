package com.fashionsystem.fashion_system.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/** Shared Redis cache configuration for all application modules. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(RedisCacheProperties.class)
@ConditionalOnProperty(prefix = "redis.cache", name = "enabled", havingValue = "true")
public class RedisCacheConfig implements CachingConfigurer {

    private final RedisCacheProperties properties;
    private final RedisConnectionFactory connectionFactory;

    public RedisCacheConfig(
            RedisCacheProperties properties,
            RedisConnectionFactory connectionFactory) {
        this.properties = properties;
        this.connectionFactory = connectionFactory;
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaults = baseConfiguration(properties.getDefaultTtl());
        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(
                connectionFactory, BatchStrategies.scan(properties.getScanBatchSize()));
        return RedisCacheManager.builder(writer)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigurations())
                .disableCreateOnMissingCache()
                .transactionAware()
                .enableStatistics()
                .build();
    }

    Map<String, RedisCacheConfiguration> cacheConfigurations() {
        Map<String, RedisCacheConfiguration> configurations = new LinkedHashMap<>();
        add(configurations, properties.getActiveModulesTtl(), CacheNames.ACTIVE_MODULES);
        add(configurations, properties.getAuthorizationCatalogTtl(),
                CacheNames.AUTHORIZATION_MODULE_LIST,
                CacheNames.AUTHORIZATION_MODULE_DETAIL,
                CacheNames.AUTHORIZATION_GROUP_LIST,
                CacheNames.AUTHORIZATION_GROUP_DETAIL,
                CacheNames.AUTHORIZATION_PERMISSION_LIST,
                CacheNames.AUTHORIZATION_PERMISSION_DETAIL,
                CacheNames.AUTHORIZATION_CATALOG);
        add(configurations, properties.getAuthorizationRoleTtl(),
                CacheNames.AUTHORIZATION_ROLE_LIST,
                CacheNames.AUTHORIZATION_ROLE_DETAIL);
        add(configurations, properties.getAuthorizationEffectivePermissionTtl(),
                CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS);
        add(configurations, properties.getAuthorizationUserAssignmentTtl(),
                CacheNames.AUTHORIZATION_USER_ROLE_LIST,
                CacheNames.AUTHORIZATION_USER_PERMISSION_LIST);
        return Map.copyOf(configurations);
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler(properties.getWarningInterval());
    }

    private RedisCacheConfiguration baseConfiguration(Duration ttl) {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.fashionsystem.fashion_system.dto.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.util.")
                .allowIfSubType(Enum.class)
                .allowIfSubTypeIsArray()
                .build();
        var valueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
        String prefix = normalizedPrefix(properties.getKeyPrefix());

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> prefix + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(StringRedisSerializer.UTF_8))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));
    }

    private void add(
            Map<String, RedisCacheConfiguration> configurations,
            Duration ttl,
            String... cacheNames) {
        RedisCacheConfiguration configuration = baseConfiguration(ttl);
        for (String cacheName : cacheNames) {
            configurations.put(cacheName, configuration);
        }
    }

    private String normalizedPrefix(String configuredPrefix) {
        String prefix = configuredPrefix == null || configuredPrefix.isBlank()
                ? "fs:local:v1:"
                : configuredPrefix.trim();
        return prefix.endsWith(":") ? prefix : prefix + ':';
    }
}
