package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.dto.ProductImageDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CatalogReadCacheServiceTest.CacheTestConfiguration.class)
class CatalogReadCacheServiceTest {

    @Autowired CatalogReadCacheService cacheService;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void repeatedPageReadUsesTheFirstLoaderOnly() {
        AtomicInteger loads = new AtomicInteger();
        BrandDto expected = BrandDto.builder().id(UUID.randomUUID()).name("Acme").build();
        var pageable = PageRequest.of(0, 20);

        var first = cacheService.brands("same-key", () -> {
            loads.incrementAndGet();
            return new PageImpl<>(List.of(expected), pageable, 1);
        });
        var second = cacheService.brands("same-key", () -> {
            loads.incrementAndGet();
            return new PageImpl<>(List.of(), pageable, 0);
        });

        assertThat(first.toPage().getContent()).containsExactly(expected);
        assertThat(second.toPage().getContent()).containsExactly(expected);
        assertThat(loads).hasValue(1);
    }

    @Test
    void differentCanonicalKeysUseIndependentEntries() {
        AtomicInteger loads = new AtomicInteger();
        var pageable = PageRequest.of(0, 20);

        cacheService.brands("filter-active", () -> {
            loads.incrementAndGet();
            return new PageImpl<>(List.of(), pageable, 0);
        });
        cacheService.brands("filter-inactive", () -> {
            loads.incrementAndGet();
            return new PageImpl<>(List.of(), pageable, 0);
        });

        assertThat(loads).hasValue(2);
    }

    @Test
    void listCacheDetachesValueFromLoaderCollection() {
        ProductImageDto image = ProductImageDto.builder().id(UUID.randomUUID()).build();
        List<ProductImageDto> source = new ArrayList<>(List.of(image));

        List<ProductImageDto> cached = cacheService.productImages("variant-key", () -> source);
        source.clear();

        assertThat(cached).containsExactly(image);
        assertThat(cacheService.productImages("variant-key", List::of)).containsExactly(image);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.BRAND_LIST, CacheNames.PRODUCT_IMAGE_LIST);
        }

        @Bean
        CatalogReadCacheService catalogReadCacheService() {
            return new CatalogReadCacheService();
        }
    }
}
