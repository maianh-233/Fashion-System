package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.SupplierDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Supplier;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.SupplierMapper;
import com.fashionsystem.fashion_system.repository.SupplierRepository;
import java.time.LocalDateTime;
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

/** Cung cấp nghiệp vụ quản lý nhà cung cấp. */
@Service
@RequiredArgsConstructor
public class SupplierService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "code", "name", "contactName", "phone", "email", "status", "createdAt", "updatedAt");
    private final SupplierRepository repository;
    private final SupplierMapper mapper;
    private final CatalogIdentityService identityService;

    /** Tạo mới nhà cung cấp. */
    @Transactional
    public SupplierDto create(SupplierDto request) {
        validateAndEnsureUniqueContacts(request, null);
        Supplier entity = mapper.toEntity(request);
        entity.setCode(identityService.nextSupplierCode());
        entity.setStatus("ACTIVE");
        try {
            return mapper.toDto(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Mã, email hoặc số điện thoại nhà cung cấp đã tồn tại");
        }
    }

    /** Lấy chi tiết nhà cung cấp theo ID. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.SUPPLIER_DETAIL, key = "#id")
    public SupplierDto getById(UUID id) { return mapper.toDto(requireSupplier(id)); }

    /** Lấy danh sách nhà cung cấp với tìm kiếm, lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<SupplierDto> getList(String keyword, String status, Pageable pageable) {
        validateSort(pageable);
        return repository.search(trimToEmpty(keyword), normalizeFilter(status), pageable).map(mapper::toDto);
    }

    /** Cập nhật nhà cung cấp. */
    @Transactional
    @CachePut(cacheNames = CacheNames.SUPPLIER_DETAIL, key = "#id")
    public SupplierDto update(UUID id, SupplierDto request) {
        Supplier entity = requireSupplier(id);
        if ("DELETED".equalsIgnoreCase(entity.getStatus())) {
            throw BusinessException.invalidState("Hãy khôi phục nhà cung cấp trước khi cập nhật");
        }
        if ("DELETED".equalsIgnoreCase(request.getStatus())) {
            throw BusinessException.badRequest("Chỉ có thể khôi phục nhà cung cấp qua thao tác khôi phục");
        }
        validateAndEnsureUniqueContacts(request, id);
        mapper.updateEntity(request, entity);
        try {
            return mapper.toDto(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Email hoặc số điện thoại nhà cung cấp đã tồn tại");
        }
    }

    /** Xóa mềm nhà cung cấp. */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.SUPPLIER_DETAIL, key = "#id")
    public void delete(UUID id) {
        Supplier entity = requireSupplier(id);
        entity.setStatus("DELETED");
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
    }

    /** Khôi phục nhà cung cấp về trạng thái hoạt động. */
    @Transactional
    @CachePut(cacheNames = CacheNames.SUPPLIER_DETAIL, key = "#id")
    public SupplierDto restore(UUID id) {
        Supplier entity = requireSupplier(id);
        entity.setStatus("ACTIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        return mapper.toDto(repository.save(entity));
    }

    private Supplier requireSupplier(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Nhà cung cấp không tồn tại"));
    }
    private void validateAndEnsureUniqueContacts(SupplierDto request, UUID excludedId) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        if (phone != null && !phone.matches("\\+?\\d{9,15}")) {
            throw BusinessException.badRequest("Số điện thoại nhà cung cấp không hợp lệ");
        }
        boolean emailExists = email != null && (excludedId == null
                ? repository.existsByEmail(email) : repository.existsByEmailAndIdNot(email, excludedId));
        if (emailExists) throw BusinessException.conflict("Email nhà cung cấp đã tồn tại");
        boolean phoneExists = phone != null && (excludedId == null
                ? repository.existsByPhone(phone) : repository.existsByPhoneAndIdNot(phone, excludedId));
        if (phoneExists) throw BusinessException.conflict("Số điện thoại nhà cung cấp đã tồn tại");
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp nhà cung cấp không hợp lệ");
    }
    private String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
    private String normalizePhone(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("[\\s()-]", "");
    }
    private String normalizeFilter(String value) {
        String normalized = trimToEmpty(value).toUpperCase(Locale.ROOT);
        return "ALL".equals(normalized) ? "" : normalized;
    }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
