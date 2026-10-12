package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheKeys;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.dto.ProductTagDto;
import com.fashionsystem.fashion_system.dto.ProductTagMappingDto;
import com.fashionsystem.fashion_system.entity.ProductTagMapping;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductMapper;
import com.fashionsystem.fashion_system.mapper.ProductTagMapper;
import com.fashionsystem.fashion_system.mapper.ProductTagMappingMapper;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductTagMappingRepository;
import com.fashionsystem.fashion_system.repository.ProductTagRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ gắn và gỡ nhãn trên sản phẩm. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PRODUCT")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductTagMappingService {
    private static final Set<String> TAG_SORT_FIELDS = Set.of("id", "name", "createdAt");
    private static final Set<String> PRODUCT_SORT_FIELDS = Set.of(
            "id", "name", "slug", "status", "createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final ProductTagRepository tagRepository;
    private final ProductTagMappingRepository mappingRepository;
    private final ProductMapper productMapper;
    private final ProductTagMapper tagMapper;
    private final ProductTagMappingMapper mappingMapper;
    private final CatalogReadCacheService cacheReads;

    /**
     * Gắn một nhãn vào sản phẩm và từ chối mapping trùng lặp.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PRODUCT_LIST, CacheNames.PRODUCT_DETAIL,
            CacheNames.PRODUCT_TAG_LIST}, allEntries = true)
    public ProductTagMappingDto link(UUID productId, UUID tagId) {
        requireProduct(productId);
        var tag = tagRepository.findById(tagId)
                .orElseThrow(() -> BusinessException.notFound("Nhãn sản phẩm không tồn tại"));
        if (!Boolean.TRUE.equals(tag.getActive())) {
            throw BusinessException.conflict("Nhãn sản phẩm đã bị vô hiệu hóa");
        }
        if (mappingRepository.existsByProductIdAndTagId(productId, tagId)) {
            throw BusinessException.conflict("Nhãn đã được gắn với sản phẩm");
        }
        ProductTagMapping entity = ProductTagMapping.builder()
                .productId(productId)
                .tagId(tagId)
                .build();
        return mappingMapper.toDto(mappingRepository.save(entity));
    }

    /**
     * Gỡ một nhãn khỏi sản phẩm.
     */
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.PRODUCT_LIST, CacheNames.PRODUCT_DETAIL,
            CacheNames.PRODUCT_TAG_LIST}, allEntries = true)
    public void unlink(UUID productId, UUID tagId) {
        requireProduct(productId);
        requireTag(tagId);
        if (!mappingRepository.existsByProductIdAndTagId(productId, tagId)) {
            throw BusinessException.notFound("Sản phẩm chưa được gắn nhãn này");
        }
        mappingRepository.deleteByProductIdAndTagId(productId, tagId);
    }

    /**
     * Lấy các nhãn đã gắn với sản phẩm theo phân trang.
     */
    @Transactional(readOnly = true)
    public Page<ProductTagDto> getTags(UUID productId, Pageable pageable) {
        requireProduct(productId);
        validateSort(pageable, TAG_SORT_FIELDS, "nhãn");
        return cacheReads.productTags(CacheKeys.of("product-tags", productId, pageable),
                () -> mappingRepository.findTagsByProductId(productId, pageable).map(tagMapper::toDto))
                .toPage();
    }

    /**
     * Lấy các sản phẩm được gắn một nhãn theo phân trang.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(UUID tagId, Pageable pageable) {
        requireTag(tagId);
        validateSort(pageable, PRODUCT_SORT_FIELDS, "sản phẩm theo nhãn");
        return cacheReads.products(CacheKeys.of("tag-products", tagId, pageable),
                () -> mappingRepository.findProductsByTagId(tagId, pageable).map(productMapper::toDto))
                .toPage();
    }

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw BusinessException.notFound("Sản phẩm không tồn tại");
        }
    }

    private void requireTag(UUID tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw BusinessException.notFound("Nhãn sản phẩm không tồn tại");
        }
    }

    private void validateSort(Pageable pageable, Set<String> allowedFields, String subject) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !allowedFields.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp " + subject + " không hợp lệ");
    }
}
