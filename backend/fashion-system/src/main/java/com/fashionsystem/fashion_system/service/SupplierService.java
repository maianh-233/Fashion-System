package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.SupplierDto;
import com.fashionsystem.fashion_system.entity.Supplier;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.SupplierMapper;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý nhà cung cấp. */
@Service
@RequiredArgsConstructor
public class SupplierService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "code", "name", "contactName", "phone", "email", "status", "createdAt", "updatedAt");
    private final SupplierRepository repository;
    private final SupplierMapper mapper;

    /** Tạo mới nhà cung cấp. */
    @Transactional
    public SupplierDto create(SupplierDto request) {
        ensureCodeAvailable(request.getCode(), null);
        return mapper.toDto(repository.save(mapper.toEntity(request)));
    }

    /** Lấy chi tiết nhà cung cấp theo ID. */
    @Transactional(readOnly = true)
    public SupplierDto getById(UUID id) { return mapper.toDto(requireSupplier(id)); }

    /** Lấy danh sách nhà cung cấp với tìm kiếm, lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<SupplierDto> getList(String keyword, String status, Pageable pageable) {
        validateSort(pageable);
        return repository.search(trimToEmpty(keyword), normalizeFilter(status), pageable).map(mapper::toDto);
    }

    /** Cập nhật nhà cung cấp. */
    @Transactional
    public SupplierDto update(UUID id, SupplierDto request) {
        Supplier entity = requireSupplier(id);
        ensureCodeAvailable(request.getCode(), id);
        mapper.updateEntity(request, entity);
        return mapper.toDto(repository.save(entity));
    }

    /** Xóa nhà cung cấp chưa có phiếu nhập tham chiếu. */
    @Transactional
    public void delete(UUID id) {
        try {
            repository.delete(requireSupplier(id));
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa nhà cung cấp đang được sử dụng");
        }
    }

    private Supplier requireSupplier(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Nhà cung cấp không tồn tại"));
    }
    private void ensureCodeAvailable(String code, UUID excludedId) {
        String value = normalizeCode(code);
        if (value == null) return;
        boolean exists = excludedId == null ? repository.existsByCode(value) : repository.existsByCodeAndIdNot(value, excludedId);
        if (exists) throw BusinessException.conflict("Mã nhà cung cấp đã tồn tại");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp nhà cung cấp không hợp lệ");
    }
    private String normalizeCode(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
    private String normalizeFilter(String value) { return trimToEmpty(value).toUpperCase(Locale.ROOT); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
