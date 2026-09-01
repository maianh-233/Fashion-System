package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.config.ResilientCacheErrorHandler;
import com.fashionsystem.fashion_system.dto.ModuleDto;
import com.fashionsystem.fashion_system.entity.Module;
import com.fashionsystem.fashion_system.mapper.ModuleMapper;
import com.fashionsystem.fashion_system.repository.ModuleRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RedisFailureFallbackTest.FailureTestConfiguration.class)
class RedisFailureFallbackTest {

    @Autowired private ModuleService moduleService;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private ModuleMapper moduleMapper;

    @Test
    void redisGetAndPutFailuresFallBackToDatabase() {
        Module entity = Module.builder().code("CATALOG").name("Catalog").active(true).build();
        ModuleDto response = ModuleDto.builder().code("CATALOG").name("Catalog").active(true).build();
        when(moduleRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(entity));
        when(moduleMapper.toDto(entity)).thenReturn(response);

        assertThat(moduleService.getActiveModules()).containsExactly(response);

        verify(moduleRepository).findAllByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class FailureTestConfiguration implements CachingConfigurer {
        private final Cache failingCache = failingCache();

        @Bean
        @Override
        public CacheManager cacheManager() {
            CacheManager manager = mock(CacheManager.class);
            when(manager.getCache(CacheNames.ACTIVE_MODULES)).thenReturn(failingCache);
            when(manager.getCacheNames()).thenReturn(List.of(CacheNames.ACTIVE_MODULES));
            return manager;
        }

        @Bean
        @Override
        public CacheErrorHandler errorHandler() {
            return new ResilientCacheErrorHandler(Duration.ofMinutes(1));
        }

        @Bean ModuleRepository moduleRepository() { return mock(ModuleRepository.class); }
        @Bean ModuleMapper moduleMapper() { return mock(ModuleMapper.class); }

        @Bean
        ModuleService moduleService(ModuleRepository repository, ModuleMapper mapper) {
            return new ModuleService(repository, mapper);
        }

        private Cache failingCache() {
            Cache cache = mock(Cache.class);
            when(cache.getName()).thenReturn(CacheNames.ACTIVE_MODULES);
            when(cache.get(any())).thenThrow(new IllegalStateException("Redis unavailable"));
            doThrow(new IllegalStateException("Redis unavailable")).when(cache).put(any(), any());
            return cache;
        }
    }
}
