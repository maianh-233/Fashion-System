package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.dto.WarehouseLookupDto;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.CategoryRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import java.util.List;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WarehouseLookupCacheServiceTest.CacheTestConfiguration.class)
class WarehouseLookupCacheServiceTest {

    @Autowired WarehouseLookupService service;
    @Autowired StoreAccessService access;
    @Autowired ProductRepository products;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        reset(access, products);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void cacheHitStillRequiresAuthorization() {
        UUID actor = UUID.randomUUID();
        UUID store = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Pageable requestPage = PageRequest.of(0, 20);
        when(access.requireAny(eq(actor), eq(store), any())).thenReturn(store)
                .thenThrow(BusinessException.forbidden("denied"));
        when(products.search(eq("shirt"), eq(null), eq(null), eq(null), eq(null),
                eq(""), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Product.builder()
                        .id(productId).name("Shirt").code("SP1").build())));

        assertThat(service.products(actor, store, " shirt ", requestPage).getContent())
                .extracting(WarehouseLookupDto.ProductOption::id)
                .containsExactly(productId);
        assertThatThrownBy(() -> service.products(actor, store, "shirt", requestPage))
                .isInstanceOf(BusinessException.class);

        verify(access, times(2)).requireAny(eq(actor), eq(store), any());
        verify(products, times(1)).search(eq("shirt"), eq(null), eq(null), eq(null), eq(null),
                eq(""), eq(""), any(Pageable.class));
    }

    @Test
    void resolvedStoreIsPartOfTheCacheKey() {
        UUID actor = UUID.randomUUID();
        UUID firstStore = UUID.randomUUID();
        UUID secondStore = UUID.randomUUID();
        Pageable requestPage = PageRequest.of(0, 20);
        when(access.requireAny(eq(actor), eq(firstStore), any())).thenReturn(firstStore);
        when(access.requireAny(eq(actor), eq(secondStore), any())).thenReturn(secondStore);
        when(products.search(eq(""), eq(null), eq(null), eq(null), eq(null),
                eq(""), eq(""), any(Pageable.class))).thenReturn(Page.empty());

        service.products(actor, firstStore, null, requestPage);
        service.products(actor, secondStore, null, requestPage);

        verify(products, times(2)).search(eq(""), eq(null), eq(null), eq(null), eq(null),
                eq(""), eq(""), any(Pageable.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.WAREHOUSE_PRODUCT_LOOKUP,
                    CacheNames.WAREHOUSE_VARIANT_LOOKUP, CacheNames.WAREHOUSE_SUPPLIER_LOOKUP,
                    CacheNames.WAREHOUSE_CATEGORY_LOOKUP);
        }
        @Bean StoreAccessService storeAccessService() { return mock(StoreAccessService.class); }
        @Bean ProductRepository productRepository() { return mock(ProductRepository.class); }
        @Bean ProductVariantRepository productVariantRepository() { return mock(ProductVariantRepository.class); }
        @Bean SupplierRepository supplierRepository() { return mock(SupplierRepository.class); }
        @Bean CategoryRepository categoryRepository() { return mock(CategoryRepository.class); }
        @Bean CatalogReadCacheService catalogReadCacheService() { return new CatalogReadCacheService(); }
        @Bean WarehouseLookupService warehouseLookupService(StoreAccessService access,
                ProductRepository products, ProductVariantRepository variants,
                SupplierRepository suppliers, CategoryRepository categories,
                CatalogReadCacheService cacheReads) {
            return new WarehouseLookupService(access, products, variants, suppliers, categories, cacheReads);
        }
    }
}
