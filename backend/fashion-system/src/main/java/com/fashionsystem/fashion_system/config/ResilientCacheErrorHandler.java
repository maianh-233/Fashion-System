package com.fashionsystem.fashion_system.config;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/** Keeps read APIs available when Redis cache operations fail. */
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ResilientCacheErrorHandler.class);

    private final long warningIntervalMillis;
    private final ConcurrentMap<String, Long> lastWarningByOperation = new ConcurrentHashMap<>();

    public ResilientCacheErrorHandler(Duration warningInterval) {
        this.warningIntervalMillis = Math.max(1, warningInterval.toMillis());
    }

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        warn("GET", cache, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        warn("PUT", cache, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        warn("EVICT", cache, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        warn("CLEAR", cache, exception);
    }

    private void warn(String operation, Cache cache, RuntimeException exception) {
        String cacheName = cache == null ? "unknown" : cache.getName();
        String warningKey = operation + ':' + cacheName + ':' + exception.getClass().getName();
        long now = System.currentTimeMillis();
        Long previous = lastWarningByOperation.get(warningKey);
        if (previous != null && now - previous < warningIntervalMillis) {
            return;
        }
        lastWarningByOperation.put(warningKey, now);
        log.warn("Redis cache operation failed; database fallback remains active. operation={}, cache={}, error={}",
                operation, cacheName, exception.getClass().getSimpleName());
    }
}
