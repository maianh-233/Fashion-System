package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.StoreMapper;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý cửa hàng. */
@Service
@RequiredArgsConstructor
public class StoreService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "code", "name", "phone", "active", "createdAt", "updatedAt");
    private final StoreRepository repository;
    private final StoreMapper mapper;

    /** Tạo mới một cửa hàng. */
    @Transactional
    public StoreDto create(StoreDto request) {
        ensureCodeAvailable(request.getCode(), null);
        return mapper.toDto(repository.save(mapper.toEntity(request)));
    }

    /** Lấy chi tiết cửa hàng theo ID. */
    @Transactional(readOnly = true)
    public StoreDto getById(UUID id) { return mapper.toDto(requireStore(id)); }

    /** Lấy danh sách cửa hàng với tìm kiếm, lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<StoreDto> getList(String keyword, Boolean active, Pageable pageable) {
        validateSort(pageable);
        return repository.search(trimToEmpty(keyword), active, pageable).map(mapper::toDto);
    }

    /** Cập nhật thông tin cửa hàng. */
    @Transactional
    public StoreDto update(UUID id, StoreDto request) {
        Store entity = requireStore(id);
        ensureCodeAvailable(request.getCode(), id);
        mapper.updateEntity(request, entity);
        return mapper.toDto(repository.save(entity));
    }

    /** Xóa cửa hàng chưa có dữ liệu nghiệp vụ tham chiếu. */
    @Transactional
    public void delete(UUID id) {
        try {
            repository.delete(requireStore(id));
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa cửa hàng đang được sử dụng");
        }
    }

    private Store requireStore(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Cửa hàng không tồn tại"));
    }
    private void ensureCodeAvailable(String code, UUID excludedId) {
        String value = normalizeCode(code);
        if (value == null) return;
        boolean exists = excludedId == null ? repository.existsByCode(value) : repository.existsByCodeAndIdNot(value, excludedId);
        if (exists) throw BusinessException.conflict("Mã cửa hàng đã tồn tại");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp cửa hàng không hợp lệ");
    }
    private String normalizeCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
