package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Collection;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CollectionMapper;
import com.fashionsystem.fashion_system.repository.BrandRepository;
import com.fashionsystem.fashion_system.repository.CollectionRepository;
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

/** Cung cấp nghiệp vụ quản lý bộ sưu tập. */
@Service
@RequiredArgsConstructor
public class CollectionService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "brandId", "name", "code", "season", "year", "releaseDate",
            "status", "createdAt", "updatedAt");

    private final CollectionRepository collectionRepository;
    private final BrandRepository brandRepository;
    private final CollectionMapper collectionMapper;

    /**
     * Tạo mới một bộ sưu tập.
     */
    @Transactional
    public CollectionDto create(CollectionDto request) {
        requireBrand(request.getBrandId());
        ensureCodeAvailable(request.getCode(), null);
        return collectionMapper.toDto(collectionRepository.save(collectionMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết bộ sưu tập theo ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.COLLECTION_DETAIL, key = "#id")
    public CollectionDto getById(UUID id) {
        return collectionMapper.toDto(requireCollection(id));
    }

    /**
     * Lấy danh sách bộ sưu tập với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<CollectionDto> getList(
            String keyword,
            UUID brandId,
            String season,
            Integer year,
            String status,
            Pageable pageable) {
        validateSort(pageable);
        return collectionRepository.search(
                        trimToEmpty(keyword), brandId, normalizeFilter(season), year,
                        normalizeFilter(status), pageable)
                .map(collectionMapper::toDto);
    }

    /**
     * Cập nhật bộ sưu tập theo ID.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.COLLECTION_DETAIL, key = "#id")
    public CollectionDto update(UUID id, CollectionDto request) {
        Collection entity = requireCollection(id);
        requireBrand(request.getBrandId());
        ensureCodeAvailable(request.getCode(), id);
        collectionMapper.updateEntity(request, entity);
        return collectionMapper.toDto(collectionRepository.save(entity));
    }

    /**
     * Xóa bộ sưu tập theo ID.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.COLLECTION_DETAIL, key = "#id")
    public void delete(UUID id) {
        Collection entity = requireCollection(id);
        try {
            collectionRepository.delete(entity);
            collectionRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa bộ sưu tập đang được sử dụng");
        }
    }

    private Collection requireCollection(UUID id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Bộ sưu tập không tồn tại"));
    }

    private void requireBrand(UUID brandId) {
        if (brandId != null && !brandRepository.existsById(brandId)) {
            throw BusinessException.notFound("Thương hiệu không tồn tại");
        }
    }

    private void ensureCodeAvailable(String code, UUID excludedId) {
        String normalized = normalizeCode(code);
        if (normalized == null) return;
        boolean exists = excludedId == null
                ? collectionRepository.existsByCode(normalized)
                : collectionRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Mã bộ sưu tập đã tồn tại");
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp bộ sưu tập không hợp lệ");
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
