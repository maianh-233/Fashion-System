package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.entity.Brand;
import com.fashionsystem.fashion_system.mapper.BrandMapper;
import com.fashionsystem.fashion_system.repository.BrandRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BusinessCatalogReadCacheServiceTest.CacheTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BusinessCatalogReadCacheServiceTest {

    @Autowired private BrandService brandService;
    @Autowired private BrandRepository brandRepository;
    @Autowired private BrandMapper brandMapper;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        reset(brandRepository, brandMapper);
        cacheManager.getCache(CacheNames.BRAND_DETAIL).clear();
    }

    @Test
    void brandDetailReadsDatabaseOnlyOnce() {
        UUID id = UUID.randomUUID();
        Brand entity = Brand.builder().id(id).name("Acme").build();
        BrandDto response = BrandDto.builder().id(id).name("Acme").build();
        when(brandRepository.findById(id)).thenReturn(Optional.of(entity));
        when(brandMapper.toDto(entity)).thenReturn(response);

        assertThat(brandService.getById(id)).isEqualTo(response);
        assertThat(brandService.getById(id)).isEqualTo(response);

        verify(brandRepository, times(1)).findById(id);
    }

    @Test
    void brandUpdateRefreshesCachedDetail() {
        UUID id = UUID.randomUUID();
        Brand entity = Brand.builder().id(id).name("Old name").build();
        BrandDto cached = BrandDto.builder().id(id).name("Old name").build();
        BrandDto request = BrandDto.builder().id(id).name("New name").build();
        BrandDto updated = BrandDto.builder().id(id).name("New name").build();
        when(brandRepository.findById(id)).thenReturn(Optional.of(entity));
        when(brandRepository.save(entity)).thenReturn(entity);
        when(brandMapper.toDto(entity)).thenReturn(cached, updated);

        assertThat(brandService.getById(id)).isEqualTo(cached);
        assertThat(brandService.update(id, request)).isEqualTo(updated);
        assertThat(brandService.getById(id)).isEqualTo(updated);

        verify(brandRepository, times(2)).findById(id);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.BRAND_DETAIL);
        }
        @Bean BrandRepository brandRepository() { return mock(BrandRepository.class); }
        @Bean BrandMapper brandMapper() { return mock(BrandMapper.class); }
        @Bean BrandService brandService(BrandRepository repository, BrandMapper mapper) {
            return new BrandService(repository, mapper, mock(CatalogIdentityService.class),
                    mock(com.fashionsystem.fashion_system.repository.ProductRepository.class),
                    mock(com.fashionsystem.fashion_system.repository.ProductVariantRepository.class));
        }
    }
}
