package com.fashionsystem.fashion_system.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class ResilientCacheErrorHandlerTest {

    @Test
    void neverPropagatesRedisOperationErrorsToBusinessRequests() {
        ResilientCacheErrorHandler handler = new ResilientCacheErrorHandler(Duration.ofSeconds(30));
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn(CacheNames.ACTIVE_MODULES);
        RuntimeException redisError = new RuntimeException("redis unavailable");

        assertThatCode(() -> handler.handleCacheGetError(redisError, cache, "key")).doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCachePutError(redisError, cache, "key", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheEvictError(redisError, cache, "key")).doesNotThrowAnyException();
        assertThatCode(() -> handler.handleCacheClearError(redisError, cache)).doesNotThrowAnyException();
    }
}
