package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.BrandMapper;
import com.fashionsystem.fashion_system.mapper.CollectionMapper;
import com.fashionsystem.fashion_system.mapper.ProductMapper;
import com.fashionsystem.fashion_system.repository.BrandRepository;
import com.fashionsystem.fashion_system.repository.CollectionRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** Stores catalog media through the existing Cloudinary adapter. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CATALOG")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogMediaService {
    private final StorageService storage;
    private final BrandRepository brands;
    private final CollectionRepository collections;
    private final ProductRepository products;
    private final BrandMapper brandMapper;
    private final CollectionMapper collectionMapper;
    private final ProductMapper productMapper;

    @Transactional
    @CacheEvict(cacheNames = CacheNames.BRAND_DETAIL, key = "#id")
    public BrandDto uploadBrandLogo(UUID id, MultipartFile file) {
        var brand = brands.findById(id).orElseThrow(() -> BusinessException.notFound("Thương hiệu không tồn tại"));
        StorageUploadResult uploaded = storage.uploadImage(file, "brands/" + id);
        String oldPublicId = brand.getLogoPublicId();
        registerCleanup(uploaded.publicId(), oldPublicId);
        brand.setLogo(uploaded.secureUrl());
        brand.setLogoPublicId(uploaded.publicId());
        return brandMapper.toDto(brands.saveAndFlush(brand));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.COLLECTION_DETAIL, key = "#id")
    public CollectionDto uploadCollectionImage(UUID id, MultipartFile file) {
        var collection = collections.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Bộ sưu tập không tồn tại"));
        StorageUploadResult uploaded = storage.uploadImage(file, "collections/" + id);
        String oldPublicId = collection.getImagePublicId();
        registerCleanup(uploaded.publicId(), oldPublicId);
        collection.setImageUrl(uploaded.secureUrl());
        collection.setImagePublicId(uploaded.publicId());
        return collectionMapper.toDto(collections.saveAndFlush(collection));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public ProductDto uploadProductImage(UUID id, MultipartFile file) {
        var product = products.findById(id).orElseThrow(() -> BusinessException.notFound("Sản phẩm không tồn tại"));
        StorageUploadResult uploaded = storage.uploadImage(file, "products/" + id);
        String oldPublicId = product.getImagePublicId();
        registerCleanup(uploaded.publicId(), oldPublicId);
        product.setImageUrl(uploaded.secureUrl());
        product.setImagePublicId(uploaded.publicId());
        return productMapper.toDto(products.saveAndFlush(product));
    }

    private void registerCleanup(String uploadedId, String oldPublicId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                if (oldPublicId != null && !oldPublicId.isBlank()) safeDelete(oldPublicId);
            }
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safeDelete(uploadedId);
            }
        });
    }

    private void safeDelete(String publicId) {
        try { storage.deleteImage(publicId); }
        catch (RuntimeException ignored) { /* DB state remains authoritative; provider cleanup can be retried. */ }
    }
}
