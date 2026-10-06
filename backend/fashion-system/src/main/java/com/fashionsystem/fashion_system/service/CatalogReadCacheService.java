package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.dto.CachedPage;
import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.dto.ProductAttributeDto;
import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.dto.ProductTagDto;
import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.dto.PromotionDto;
import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.dto.SupplierDto;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/** Cache boundary for stable catalog and reference-data reads. Authorization stays in callers. */
@Service
public class CatalogReadCacheService {

    @Cacheable(cacheNames = CacheNames.BRAND_LIST, key = "#key")
    public CachedPage<BrandDto> brands(String key, Supplier<Page<BrandDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.CATEGORY_LIST, key = "#key")
    public CachedPage<CategoryDto> categories(String key, Supplier<Page<CategoryDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.COLLECTION_LIST, key = "#key")
    public CachedPage<CollectionDto> collections(String key, Supplier<Page<CollectionDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.PRODUCT_LIST, key = "#key")
    public CachedPage<ProductDto> products(String key, Supplier<Page<ProductDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.PRODUCT_VARIANT_LIST, key = "#key")
    public CachedPage<ProductVariantDto> productVariants(
            String key, Supplier<Page<ProductVariantDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.PRODUCT_ATTRIBUTE_LIST, key = "#key")
    public CachedPage<ProductAttributeDto> productAttributes(
            String key, Supplier<Page<ProductAttributeDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.PRODUCT_TAG_LIST, key = "#key")
    public CachedPage<ProductTagDto> productTags(String key, Supplier<Page<ProductTagDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.PROMOTION_LIST, key = "#key")
    public CachedPage<PromotionDto> promotions(String key, Supplier<Page<PromotionDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.STORE_LIST, key = "#key")
    public CachedPage<StoreDto> stores(String key, Supplier<Page<StoreDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.DEPARTMENT_LIST, key = "#key")
    public CachedPage<DepartmentDto> departments(String key, Supplier<Page<DepartmentDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.POSITION_LIST, key = "#key")
    public CachedPage<PositionDto> positions(String key, Supplier<Page<PositionDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.SUPPLIER_LIST, key = "#key")
    public CachedPage<SupplierDto> suppliers(String key, Supplier<Page<SupplierDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.CUSTOMER_TIER_LIST, key = "#key")
    public CachedPage<CustomerTierDto> customerTiers(
            String key, Supplier<Page<CustomerTierDto>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.PRODUCT_IMAGE_LIST, key = "#key")
    public List<ProductImageDto> productImages(String key, Supplier<List<ProductImageDto>> loader) {
        return new ArrayList<>(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.PRODUCT_IMAGE_DETAIL, key = "#key")
    public ProductImageDto productImage(String key, Supplier<ProductImageDto> loader) {
        return loader.get();
    }

    @Cacheable(cacheNames = CacheNames.WAREHOUSE_PRODUCT_LOOKUP, key = "#key")
    public CachedPage<WarehouseLookupService.ProductOption> warehouseProducts(
            String key, Supplier<Page<WarehouseLookupService.ProductOption>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.WAREHOUSE_VARIANT_LOOKUP, key = "#key")
    public List<WarehouseLookupService.VariantOption> warehouseVariants(
            String key, Supplier<List<WarehouseLookupService.VariantOption>> loader) {
        return new ArrayList<>(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.WAREHOUSE_SUPPLIER_LOOKUP, key = "#key")
    public CachedPage<WarehouseLookupService.SupplierOption> warehouseSuppliers(
            String key, Supplier<Page<WarehouseLookupService.SupplierOption>> loader) {
        return CachedPage.from(loader.get());
    }

    @Cacheable(cacheNames = CacheNames.WAREHOUSE_CATEGORY_LOOKUP, key = "#key")
    public List<WarehouseLookupService.CategoryOption> warehouseCategories(
            String key, Supplier<List<WarehouseLookupService.CategoryOption>> loader) {
        return new ArrayList<>(loader.get());
    }
}
