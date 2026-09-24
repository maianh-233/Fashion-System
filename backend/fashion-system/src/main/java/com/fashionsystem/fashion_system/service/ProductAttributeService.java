package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ProductAttributeDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.ProductAttribute;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductAttributeMapper;
import com.fashionsystem.fashion_system.repository.ProductAttributeRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý thuộc tính mô tả của sản phẩm. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PRODUCT")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductAttributeService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "attributeName", "attributeValue", "createdAt");

    private final ProductRepository productRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeMapper attributeMapper;

    /**
     * Tạo thuộc tính cho một sản phẩm.
     */
    @Transactional
    public ProductAttributeDto create(UUID productId, ProductAttributeDto request) {
        requireProduct(productId);
        ProductAttribute entity = attributeMapper.toEntity(request);
        entity.setProductId(productId);
        return attributeMapper.toDto(attributeRepository.save(entity));
    }

    /**
     * Lấy chi tiết thuộc tính thuộc một sản phẩm.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCT_ATTRIBUTE_DETAIL,
            key = "#productId + ':' + #attributeId")
    public ProductAttributeDto getById(UUID productId, UUID attributeId) {
        requireProduct(productId);
        return attributeMapper.toDto(requireAttribute(productId, attributeId));
    }

    /**
     * Lấy thuộc tính của sản phẩm với tìm kiếm, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<ProductAttributeDto> getList(
            UUID productId, String keyword, Pageable pageable) {
        requireProduct(productId);
        validateSort(pageable);
        return attributeRepository.searchByProduct(productId, trimToEmpty(keyword), pageable)
                .map(attributeMapper::toDto);
    }

    /**
     * Cập nhật thuộc tính thuộc một sản phẩm.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_ATTRIBUTE_DETAIL,
            key = "#productId + ':' + #attributeId")
    public ProductAttributeDto update(
            UUID productId, UUID attributeId, ProductAttributeDto request) {
        requireProduct(productId);
        ProductAttribute entity = requireAttribute(productId, attributeId);
        attributeMapper.updateEntity(request, entity);
        return attributeMapper.toDto(attributeRepository.save(entity));
    }

    /**
     * Xóa thuộc tính thuộc một sản phẩm.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_ATTRIBUTE_DETAIL,
            key = "#productId + ':' + #attributeId")
    public void delete(UUID productId, UUID attributeId) {
        requireProduct(productId);
        attributeRepository.delete(requireAttribute(productId, attributeId));
    }

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw BusinessException.notFound("Sản phẩm không tồn tại");
        }
    }

    private ProductAttribute requireAttribute(UUID productId, UUID attributeId) {
        return attributeRepository.findByIdAndProductId(attributeId, productId)
                .orElseThrow(() -> BusinessException.notFound("Thuộc tính sản phẩm không tồn tại"));
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp thuộc tính không hợp lệ");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
