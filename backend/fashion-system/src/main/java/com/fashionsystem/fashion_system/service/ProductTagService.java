package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ProductTagDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.ProductTag;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductTagMapper;
import com.fashionsystem.fashion_system.repository.ProductTagRepository;
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

/** Cung cấp nghiệp vụ quản lý nhãn sản phẩm. */
@Service
@RequiredArgsConstructor
public class ProductTagService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "createdAt");

    private final ProductTagRepository tagRepository;
    private final ProductTagMapper tagMapper;

    /**
     * Tạo nhãn sản phẩm với tên duy nhất không phân biệt hoa thường.
     */
    @Transactional
    public ProductTagDto create(ProductTagDto request) {
        ensureNameAvailable(request.getName(), null);
        return tagMapper.toDto(tagRepository.save(tagMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết nhãn sản phẩm theo ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCT_TAG_DETAIL, key = "#id")
    public ProductTagDto getById(UUID id) {
        return tagMapper.toDto(requireTag(id));
    }

    /**
     * Lấy danh sách nhãn với tìm kiếm, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<ProductTagDto> getList(String keyword, Pageable pageable) {
        validateSort(pageable);
        return tagRepository.search(trimToEmpty(keyword), pageable).map(tagMapper::toDto);
    }

    /**
     * Cập nhật tên nhãn sản phẩm.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_TAG_DETAIL, key = "#id")
    public ProductTagDto update(UUID id, ProductTagDto request) {
        ProductTag entity = requireTag(id);
        ensureNameAvailable(request.getName(), id);
        tagMapper.updateEntity(request, entity);
        return tagMapper.toDto(tagRepository.save(entity));
    }

    /**
     * Xóa nhãn nếu chưa được gắn với sản phẩm.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_TAG_DETAIL, key = "#id")
    public void delete(UUID id) {
        ProductTag entity = requireTag(id);
        try {
            tagRepository.delete(entity);
            tagRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa nhãn đang được gắn với sản phẩm");
        }
    }

    private ProductTag requireTag(UUID id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Nhãn sản phẩm không tồn tại"));
    }

    private void ensureNameAvailable(String name, UUID excludedId) {
        String normalized = name.trim();
        boolean exists = excludedId == null
                ? tagRepository.existsByNameIgnoreCase(normalized)
                : tagRepository.existsByNameIgnoreCaseAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Tên nhãn sản phẩm đã tồn tại");
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp nhãn không hợp lệ");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
