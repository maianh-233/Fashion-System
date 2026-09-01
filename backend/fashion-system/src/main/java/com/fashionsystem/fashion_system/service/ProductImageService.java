package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.entity.ProductImage;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductImageMapper;
import com.fashionsystem.fashion_system.repository.ProductImageRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý hình ảnh thuộc một biến thể sản phẩm. */
@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductImageMapper imageMapper;

    /**
     * Tạo hình ảnh cho biến thể và duy trì duy nhất một ảnh primary.
     */
    @Transactional
    public ProductImageDto create(UUID variantId, ProductImageDto request) {
        lockVariant(variantId);
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            imageRepository.clearPrimary(variantId);
        }
        ProductImage entity = imageMapper.toEntity(request);
        entity.setProductVariantId(variantId);
        return imageMapper.toDto(imageRepository.save(entity));
    }

    /**
     * Lấy chi tiết hình ảnh thuộc một biến thể.
     */
    @Transactional(readOnly = true)
    public ProductImageDto getById(UUID variantId, UUID imageId) {
        requireVariant(variantId);
        return imageMapper.toDto(requireImage(variantId, imageId));
    }

    /**
     * Lấy hình ảnh của biến thể theo ảnh primary và thứ tự hiển thị.
     */
    @Transactional(readOnly = true)
    public List<ProductImageDto> getList(UUID variantId) {
        requireVariant(variantId);
        return imageRepository
                .findAllByProductVariantIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(variantId)
                .stream()
                .map(imageMapper::toDto)
                .toList();
    }

    /**
     * Cập nhật hình ảnh thuộc một biến thể và đồng bộ ảnh primary.
     */
    @Transactional
    public ProductImageDto update(UUID variantId, UUID imageId, ProductImageDto request) {
        lockVariant(variantId);
        ProductImage entity = requireImage(variantId, imageId);
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            imageRepository.clearPrimary(variantId);
            entity = requireImage(variantId, imageId);
        }
        imageMapper.updateEntity(request, entity);
        return imageMapper.toDto(imageRepository.save(entity));
    }

    /**
     * Xóa hình ảnh thuộc một biến thể.
     */
    @Transactional
    public void delete(UUID variantId, UUID imageId) {
        lockVariant(variantId);
        imageRepository.delete(requireImage(variantId, imageId));
    }

    /**
     * Thiết lập hình ảnh primary duy nhất cho biến thể.
     */
    @Transactional
    public ProductImageDto setPrimary(UUID variantId, UUID imageId) {
        lockVariant(variantId);
        requireImage(variantId, imageId);
        imageRepository.clearPrimary(variantId);
        ProductImage entity = requireImage(variantId, imageId);
        entity.setIsPrimary(Boolean.TRUE);
        return imageMapper.toDto(imageRepository.save(entity));
    }

    private void requireVariant(UUID variantId) {
        if (!variantRepository.existsById(variantId)) {
            throw BusinessException.notFound("Biến thể sản phẩm không tồn tại");
        }
    }

    private void lockVariant(UUID variantId) {
        variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> BusinessException.notFound("Biến thể sản phẩm không tồn tại"));
    }

    private ProductImage requireImage(UUID variantId, UUID imageId) {
        return imageRepository.findByIdAndProductVariantId(imageId, variantId)
                .orElseThrow(() -> BusinessException.notFound("Hình ảnh sản phẩm không tồn tại"));
    }
}
