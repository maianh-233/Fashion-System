package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.entity.Category;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CategoryMapper;
import com.fashionsystem.fashion_system.repository.CategoryRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý danh mục. */
@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "parentId", "name", "code", "createdAt", "updatedAt");

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Tạo mới một danh mục.
     */
    @Transactional
    public CategoryDto create(CategoryDto request) {
        requireParent(request.getParentId(), null);
        ensureCodeAvailable(request.getCode(), null);
        return categoryMapper.toDto(categoryRepository.save(categoryMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết danh mục theo ID.
     */
    @Transactional(readOnly = true)
    public CategoryDto getById(UUID id) {
        return categoryMapper.toDto(requireCategory(id));
    }

    /**
     * Lấy danh sách danh mục với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CategoryDto> getList(String keyword, UUID parentId, Pageable pageable) {
        validateSort(pageable);
        return categoryRepository.search(trimToEmpty(keyword), parentId, pageable)
                .map(categoryMapper::toDto);
    }

    /**
     * Cập nhật danh mục theo ID.
     */
    @Transactional
    public CategoryDto update(UUID id, CategoryDto request) {
        Category entity = requireCategory(id);
        requireParent(request.getParentId(), id);
        ensureCodeAvailable(request.getCode(), id);
        categoryMapper.updateEntity(request, entity);
        return categoryMapper.toDto(categoryRepository.save(entity));
    }

    /**
     * Xóa danh mục theo ID.
     */
    @Transactional
    public void delete(UUID id) {
        Category entity = requireCategory(id);
        try {
            categoryRepository.delete(entity);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa danh mục đang được sử dụng");
        }
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
        if (!categoryRepository.existsById(parentId)) {
            throw BusinessException.notFound("Danh mục cha không tồn tại");
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
