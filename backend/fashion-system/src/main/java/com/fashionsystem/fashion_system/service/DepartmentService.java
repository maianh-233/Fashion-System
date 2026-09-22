package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.DepartmentMapper;
import com.fashionsystem.fashion_system.repository.DepartmentRepository;
import com.fashionsystem.fashion_system.repository.UserDepartmentRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý phòng ban. */
@Service
@RequiredArgsConstructor
public class DepartmentService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "code", "name", "active", "createdAt", "updatedAt");

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final UserDepartmentRepository userDepartmentRepository;

    /**
     * Tạo mới một phòng ban.
     */
    @Transactional
    public DepartmentDto create(DepartmentDto request) {
        ensureCodeAvailable(request.getCode(), null);
        return departmentMapper.toDto(departmentRepository.save(departmentMapper.toEntity(request)));
    }

    /**
     * Lấy chi tiết phòng ban theo ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.DEPARTMENT_DETAIL, key = "#id")
    public DepartmentDto getById(UUID id) {
        return departmentMapper.toDto(requireDepartment(id));
    }

    /**
     * Lấy danh sách phòng ban với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<DepartmentDto> getList(String keyword, Boolean active, Pageable pageable) {
        validateSort(pageable);
        return departmentRepository.search(trimToEmpty(keyword), active, pageable)
                .map(departmentMapper::toDto);
    }

    /**
     * Cập nhật phòng ban theo ID.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.DEPARTMENT_DETAIL, key = "#id")
    public DepartmentDto update(UUID id, DepartmentDto request) {
        Department entity = requireDepartment(id);
        if (Boolean.TRUE.equals(entity.getActive()) && Boolean.FALSE.equals(request.getActive())) {
            ensureCanDeactivate(id);
        }
        departmentMapper.updateMutableFields(request, entity);
        return departmentMapper.toDto(departmentRepository.save(entity));
    }

    /**
     * Xóa phòng ban theo ID.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.DEPARTMENT_DETAIL, key = "#id")
    public void delete(UUID id) {
        Department entity = requireDepartment(id);
        ensureCanDeactivate(id);
        entity.setActive(Boolean.FALSE);
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        departmentRepository.save(entity);
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.DEPARTMENT_DETAIL, key = "#id")
    public DepartmentDto restore(UUID id) {
        Department entity = requireDepartment(id);
        entity.setActive(Boolean.TRUE);
        entity.setUpdatedAt(java.time.LocalDateTime.now());
        return departmentMapper.toDto(departmentRepository.save(entity));
    }

    private void ensureCanDeactivate(UUID id) {
        if (userDepartmentRepository.existsActiveEmployeeByDepartmentId(id)) {
            throw BusinessException.invalidState(
                    "Không thể xóa phòng ban vì vẫn còn nhân viên đang hoạt động");
        }
    }

    private Department requireDepartment(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Phòng ban không tồn tại"));
    }

    private void ensureCodeAvailable(String code, UUID excludedId) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null
                ? departmentRepository.existsByCode(normalized)
                : departmentRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Mã phòng ban đã tồn tại");
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp phòng ban không hợp lệ");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
