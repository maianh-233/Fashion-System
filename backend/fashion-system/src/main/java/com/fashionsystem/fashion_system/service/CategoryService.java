package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Category;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CategoryMapper;
import com.fashionsystem.fashion_system.repository.CategoryRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
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

/** Cung cấp nghiệp vụ quản lý danh mục. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("CATEGORY")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "parentId", "name", "code", "createdAt", "updatedAt");

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CatalogIdentityService identityService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    /**
     * Tạo mới một danh mục.
     */
    @Transactional
    public CategoryDto create(CategoryDto request) {
        requireParent(request.getParentId(), null);
        Category entity = categoryMapper.toEntity(request);
        entity.setCode(identityService.nextCategoryCode());
        entity.setActive(true);
        try {
            return categoryMapper.toDto(categoryRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Mã danh mục đã tồn tại");
        }
    }

    /**
     * Lấy chi tiết danh mục theo ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.CATEGORY_DETAIL, key = "#id")
    public CategoryDto getById(UUID id) {
        return categoryMapper.toDto(requireCategory(id));
    }

    /**
     * Lấy danh sách danh mục với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CategoryDto> getList(String keyword, UUID parentId, Boolean active, Pageable pageable) {
        validateSort(pageable);
        return categoryRepository.search(trimToEmpty(keyword), parentId, active, pageable)
                .map(categoryMapper::toDto);
    }

    /**
     * Cập nhật danh mục theo ID.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.CATEGORY_DETAIL, key = "#id")
    public CategoryDto update(UUID id, CategoryDto request) {
        Category entity = requireCategory(id);
        requireParent(request.getParentId(), id);
        categoryMapper.updateEntity(request, entity);
        return categoryMapper.toDto(categoryRepository.save(entity));
    }

    /**
     * Xóa danh mục theo ID.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CATEGORY_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL, allEntries = true)})
    public void delete(UUID id) {
        Category entity = requireCategory(id);
        if (categoryRepository.existsByParentIdAndActiveTrue(id)) {
            throw BusinessException.conflict("Hãy vô hiệu hóa các danh mục con trước");
        }
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        variantRepository.deactivateByCategoryId(id);
        productRepository.archiveByCategoryId(id);
        categoryRepository.save(entity);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.CATEGORY_DETAIL, key = "#id")
    public CategoryDto restore(UUID id) {
        Category entity = requireCategory(id);
        requireParent(entity.getParentId(), id);
        entity.setActive(true);
        entity.setUpdatedAt(LocalDateTime.now());
        return categoryMapper.toDto(categoryRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public long affectedProducts(UUID id) {
        requireCategory(id);
        return productRepository.countByCategoryId(id);
    }

    @Transactional(readOnly = true)
    public long affectedChildren(UUID id) {
        requireCategory(id);
        return categoryRepository.countByParentIdAndActiveTrue(id);
    }

    private Category requireCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Danh mục không tồn tại"));
    }

    private void requireParent(UUID parentId, UUID categoryId) {
        if (parentId == null) return;
        if (parentId.equals(categoryId)) {
            throw BusinessException.badRequest("Danh mục không thể là danh mục cha của chính nó");
        }
        var visited = new HashSet<UUID>();
        UUID current = parentId;
        while (current != null) {
            if (!visited.add(current) || current.equals(categoryId)) {
                throw BusinessException.conflict("Quan hệ danh mục cha tạo vòng lặp");
            }
            Category parent = requireCategory(current);
            if (!Boolean.TRUE.equals(parent.getActive())) {
                throw BusinessException.conflict("Danh mục cha đã bị vô hiệu hóa");
            }
            current = parent.getParentId();
        }
    }

    private void ensureCodeAvailable(String code, UUID excludedId) {
        String normalized = normalizeCode(code);
        if (normalized == null) return;
        boolean exists = excludedId == null
                ? categoryRepository.existsByCode(normalized)
                : categoryRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Mã danh mục đã tồn tại");
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp danh mục không hợp lệ");
    }

    private String normalizeCode(String code) {
        String normalized = trimToNull(code);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
