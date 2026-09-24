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
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý sản phẩm và các tham chiếu danh mục. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PRODUCT")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "code", "name", "slug", "material", "fit", "gender", "status", "createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CollectionRepository collectionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductVariantRepository variantRepository;
    private final CatalogIdentityService identityService;

    /**
     * Tạo sản phẩm sau khi kiểm tra slug và các danh mục tham chiếu.
     */
    @Transactional
    public ProductDto create(ProductDto request) {
        validateReferences(request);
        Product entity = productMapper.toEntity(request);
        entity.setImageUrl("");
        entity.setCode(identityService.nextProductCode());
        String base = identityService.slugBase(entity.getName());
        String suffix = "-" + entity.getCode().toLowerCase(Locale.ROOT);
        entity.setSlug(base.substring(0, Math.min(base.length(), 255 - suffix.length())) + suffix);
        try {
            return productMapper.toDto(productRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Mã hoặc slug sản phẩm đã tồn tại");
        }
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
            UUID tagId,
            String gender,
            String status,
            Pageable pageable) {
        validateSort(pageable);
        return productRepository.search(
                        trimToEmpty(keyword), brandId, collectionId, categoryId, tagId,
                        normalizeFilter(gender), normalizeFilter(status), pageable)
                .map(productMapper::toDto);
    }

    /**
     * Cập nhật sản phẩm và kiểm tra lại slug cùng các tham chiếu.
     */
    @Transactional
    @Caching(put = @CachePut(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL, allEntries = true))
    public ProductDto update(UUID id, ProductDto request) {
        Product entity = requireProduct(id);
        validateReferences(request);
        boolean deactivateVariants = request.getStatus() != null
                && !"ACTIVE".equalsIgnoreCase(request.getStatus());
        productMapper.updateEntity(request, entity);
        if (deactivateVariants) variantRepository.deactivateByProductId(id);
        return productMapper.toDto(productRepository.save(entity));
    }

    /**
     * Xóa sản phẩm nếu chưa được variant hoặc dữ liệu nghiệp vụ khác tham chiếu.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL, allEntries = true)})
    public void delete(UUID id) {
        Product entity = requireProduct(id);
        entity.setStatus("ARCHIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        variantRepository.deactivateByProductId(id);
        productRepository.save(entity);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, key = "#id")
    public ProductDto restore(UUID id) {
        Product entity = requireProduct(id);
        ProductDto dependencies = productMapper.toDto(entity);
        validateReferences(dependencies);
        entity.setStatus("ACTIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        return productMapper.toDto(productRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public long affectedVariants(UUID id) {
        requireProduct(id);
        return variantRepository.countByProductId(id);
    }

    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm không tồn tại"));
    }

    private void validateReferences(ProductDto request) {
        if (request.getBrandId() != null) {
            var brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> BusinessException.notFound("Thương hiệu không tồn tại"));
            if (!"ACTIVE".equalsIgnoreCase(brand.getStatus()))
                throw BusinessException.conflict("Không thể kích hoạt sản phẩm vì thương hiệu đã bị vô hiệu hóa");
        }
        if (request.getCollectionId() != null) {
            var collection = collectionRepository.findById(request.getCollectionId())
                    .orElseThrow(() -> BusinessException.notFound("Bộ sưu tập không tồn tại"));
            if (!"ACTIVE".equalsIgnoreCase(collection.getStatus()))
                throw BusinessException.conflict("Bộ sưu tập đã bị vô hiệu hóa");
        }
        if (request.getCategoryId() != null) {
            var category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BusinessException.notFound("Danh mục không tồn tại"));
            if (!Boolean.TRUE.equals(category.getActive()))
                throw BusinessException.conflict("Danh mục đã bị vô hiệu hóa");
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
