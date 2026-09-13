package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Brand;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.BrandMapper;
import com.fashionsystem.fashion_system.repository.BrandRepository;
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

/** Cung cấp nghiệp vụ quản lý thương hiệu. */
@Service
@RequiredArgsConstructor
public class BrandService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "code", "status", "terminatedAt", "createdAt", "updatedAt");

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    /**
     * Tạo mới một thương hiệu.
     */
    @Transactional
    public BrandDto create(BrandDto request) {
        ensureCodeAvailable(request.getCode(), null);
        return brandMapper.toDto(brandRepository.save(brandMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết thương hiệu theo ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.BRAND_DETAIL, key = "#id")
    public BrandDto getById(UUID id) {
        return brandMapper.toDto(requireBrand(id));
    }

    /**
     * Lấy danh sách thương hiệu với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<BrandDto> getList(String keyword, String status, Pageable pageable) {
        validateSort(pageable);
        return brandRepository.search(trimToEmpty(keyword), normalizeFilter(status), pageable)
                .map(brandMapper::toDto);
    }

    /**
     * Cập nhật thương hiệu theo ID.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.BRAND_DETAIL, key = "#id")
    public BrandDto update(UUID id, BrandDto request) {
        Brand entity = requireBrand(id);
        ensureCodeAvailable(request.getCode(), id);
        brandMapper.updateEntity(request, entity);
        return brandMapper.toDto(brandRepository.save(entity));
    }

    /**
     * Xóa thương hiệu theo ID.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.BRAND_DETAIL, key = "#id")
    public void delete(UUID id) {
        Brand entity = requireBrand(id);
        try {
            brandRepository.delete(entity);
            brandRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa thương hiệu đang được sử dụng");
        }
    }

    private Brand requireBrand(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Thương hiệu không tồn tại"));
    }

    private void ensureCodeAvailable(String code, UUID excludedId) {
        String normalized = normalizeCode(code);
        if (normalized == null) return;
        boolean exists = excludedId == null
                ? brandRepository.existsByCode(normalized)
                : brandRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Mã thương hiệu đã tồn tại");
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp thương hiệu không hợp lệ");
    }

    private String normalizeCode(String code) {
        String normalized = trimToNull(code);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
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
