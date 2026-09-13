package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.entity.ProductImage;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductImageMapper;
import com.fashionsystem.fashion_system.repository.ProductImageRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Manages Product images whose URL and provider id originate only from Cloudinary. */
@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductImageMapper imageMapper;
    private final StorageService storageService;

    /** Uploads and persists a Product Variant image with compensation on persistence failure. */
    @Transactional
    public ProductImageDto upload(UUID productId, UUID variantId, MultipartFile file,
            boolean primary, int sortOrder) {
        validateSortOrder(sortOrder);
        lockVariant(productId, variantId);
        StorageUploadResult uploaded = storageService.uploadImage(
                file, storageFolder(productId, variantId));
        try {
            if (primary) imageRepository.clearPrimary(variantId);
            ProductImage entity = ProductImage.builder()
                    .productVariantId(variantId)
                    .imageUrl(uploaded.secureUrl())
                    .cloudinaryPublicId(uploaded.publicId())
                    .isPrimary(primary)
                    .sortOrder(sortOrder)
                    .createdAt(LocalDateTime.now())
                    .build();
            return imageMapper.toDto(imageRepository.save(entity));
        } catch (RuntimeException exception) {
            compensateDelete(uploaded.publicId(), exception);
            throw exception;
        }
    }

    /** Returns one image after validating the Product/Variant ownership chain. */
    @Transactional(readOnly = true)
    public ProductImageDto getById(UUID productId, UUID variantId, UUID imageId) {
        requireVariant(productId, variantId);
        return imageMapper.toDto(requireImage(variantId, imageId));
    }

    /** Lists images for a Variant after validating its Product parent. */
    @Transactional(readOnly = true)
    public List<ProductImageDto> getList(UUID productId, UUID variantId) {
        requireVariant(productId, variantId);
        return imageRepository
                .findAllByProductVariantIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(variantId)
                .stream()
                .map(imageMapper::toDto)
                .toList();
    }

    /** Replaces image binary content while retaining display metadata. */
    @Transactional
    public ProductImageDto replaceContent(UUID productId, UUID variantId, UUID imageId,
            MultipartFile file) {
        lockVariant(productId, variantId);
        ProductImage entity = requireImage(variantId, imageId);
        StorageUploadResult uploaded = storageService.uploadImage(
                file, storageFolder(productId, variantId));
        String oldPublicId = entity.getCloudinaryPublicId();
        try {
            entity.setImageUrl(uploaded.secureUrl());
            entity.setCloudinaryPublicId(uploaded.publicId());
            ProductImage saved = imageRepository.save(entity);
            if (oldPublicId != null && !oldPublicId.isBlank()) storageService.deleteImage(oldPublicId);
            return imageMapper.toDto(saved);
        } catch (RuntimeException exception) {
            compensateDelete(uploaded.publicId(), exception);
            throw exception;
        }
    }

    /** Updates only server-approved image display metadata. */
    @Transactional
    public ProductImageDto updateMetadata(UUID productId, UUID variantId, UUID imageId,
            boolean primary, int sortOrder) {
        validateSortOrder(sortOrder);
        lockVariant(productId, variantId);
        ProductImage entity = requireImage(variantId, imageId);
        if (primary) {
            imageRepository.clearPrimary(variantId);
            entity = requireImage(variantId, imageId);
        }
        entity.setIsPrimary(primary);
        entity.setSortOrder(sortOrder);
        return imageMapper.toDto(imageRepository.save(entity));
    }

    /** Deletes the Cloudinary asset identified by persisted metadata and then its association. */
    @Transactional
    public void delete(UUID productId, UUID variantId, UUID imageId) {
        lockVariant(productId, variantId);
        ProductImage entity = requireImage(variantId, imageId);
        if (entity.getCloudinaryPublicId() != null && !entity.getCloudinaryPublicId().isBlank()) {
            storageService.deleteImage(entity.getCloudinaryPublicId());
        }
        imageRepository.delete(entity);
    }

    /** Selects the unique primary image for a Variant. */
    @Transactional
    public ProductImageDto setPrimary(UUID productId, UUID variantId, UUID imageId) {
        lockVariant(productId, variantId);
        requireImage(variantId, imageId);
        imageRepository.clearPrimary(variantId);
        ProductImage entity = requireImage(variantId, imageId);
        entity.setIsPrimary(Boolean.TRUE);
        return imageMapper.toDto(imageRepository.save(entity));
    }

    private void requireVariant(UUID productId, UUID variantId) {
        if (variantRepository.findByIdAndProductId(variantId, productId).isEmpty()) {
            throw BusinessException.notFound("Biến thể sản phẩm không tồn tại");
        }
    }

    private void lockVariant(UUID productId, UUID variantId) {
        ProductVariant variant = variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> BusinessException.notFound("Biến thể sản phẩm không tồn tại"));
        if (!productId.equals(variant.getProductId())) {
            throw BusinessException.notFound("Biến thể sản phẩm không tồn tại");
        }
    }

    private ProductImage requireImage(UUID variantId, UUID imageId) {
        return imageRepository.findByIdAndProductVariantId(imageId, variantId)
                .orElseThrow(() -> BusinessException.notFound("Hình ảnh sản phẩm không tồn tại"));
    }

    private void validateSortOrder(int sortOrder) {
        if (sortOrder < 0) throw BusinessException.badRequest("Thứ tự hình ảnh không được âm");
    }

    private String storageFolder(UUID productId, UUID variantId) {
        return "products/" + productId + "/variants/" + variantId;
    }

    private void compensateDelete(String publicId, RuntimeException original) {
        try {
            storageService.deleteImage(publicId);
        } catch (RuntimeException compensationFailure) {
            original.addSuppressed(compensationFailure);
        }
    }
}
