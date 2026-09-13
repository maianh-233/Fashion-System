package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductMapper;
import com.fashionsystem.fashion_system.repository.BrandRepository;
import com.fashionsystem.fashion_system.repository.CategoryRepository;
import com.fashionsystem.fashion_system.repository.CollectionRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý sản phẩm và các tham chiếu danh mục. */
@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "slug", "material", "fit", "gender", "status", "createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CollectionRepository collectionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    /**
     * Tạo sản phẩm sau khi kiểm tra slug và các danh mục tham chiếu.
     */
    @Transactional
    public ProductDto create(ProductDto request) {
        validateReferences(request);
        ensureSlugAvailable(request.getSlug(), null);
        return productMapper.toDto(productRepository.save(productMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết sản phẩm theo ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public ProductDto getById(UUID id) {
        return productMapper.toDto(requireProduct(id));
    }

    /**
     * Lấy danh sách sản phẩm với tìm kiếm, lọc, sắp xếp và phân trang tại database.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto> getList(
            String keyword,
            UUID brandId,
            UUID collectionId,
            UUID categoryId,
            String gender,
            String status,
            Pageable pageable) {
        validateSort(pageable);
        return productRepository.search(
                        trimToEmpty(keyword), brandId, collectionId, categoryId,
                        normalizeFilter(gender), normalizeFilter(status), pageable)
                .map(productMapper::toDto);
    }

    /**
     * Cập nhật sản phẩm và kiểm tra lại slug cùng các tham chiếu.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public ProductDto update(UUID id, ProductDto request) {
        Product entity = requireProduct(id);
        validateReferences(request);
        ensureSlugAvailable(request.getSlug(), id);
        productMapper.updateEntity(request, entity);
        return productMapper.toDto(productRepository.save(entity));
    }

    /**
     * Xóa sản phẩm nếu chưa được variant hoặc dữ liệu nghiệp vụ khác tham chiếu.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public void delete(UUID id) {
        Product entity = requireProduct(id);
        try {
            productRepository.delete(entity);
            productRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa sản phẩm đang được sử dụng");
        }
    }

    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm không tồn tại"));
    }

    private void validateReferences(ProductDto request) {
        if (request.getBrandId() != null && !brandRepository.existsById(request.getBrandId())) {
            throw BusinessException.notFound("Thương hiệu không tồn tại");
        }
        if (request.getCollectionId() != null && !collectionRepository.existsById(request.getCollectionId())) {
            throw BusinessException.notFound("Bộ sưu tập không tồn tại");
        }
        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw BusinessException.notFound("Danh mục không tồn tại");
        }
    }

    private void ensureSlugAvailable(String slug, UUID excludedId) {
        String normalized = normalizeSlug(slug);
        if (normalized == null) return;
        boolean exists = excludedId == null
                ? productRepository.existsBySlug(normalized)
                : productRepository.existsBySlugAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Slug sản phẩm đã tồn tại");
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp sản phẩm không hợp lệ");
    }

    private String normalizeSlug(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeFilter(String value) {
        return trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
