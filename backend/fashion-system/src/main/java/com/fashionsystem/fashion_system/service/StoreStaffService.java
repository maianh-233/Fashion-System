package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.StoreStaffDto;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.StoreStaffMapper;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ phân công nhân viên tại cửa hàng. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("STORE")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StoreStaffService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "userId", "storeId", "staffRole", "startDate", "endDate", "active", "createdAt");
    private final StoreStaffRepository repository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreStaffMapper mapper;

    /** Tạo phân công nhân viên tại cửa hàng. */
    @Transactional
    public StoreStaffDto create(StoreStaffDto request) {
        validateRequest(request, null);
        return mapper.toDto(repository.save(mapper.toEntity(request)));
    }

    /** Lấy chi tiết một phân công nhân viên. */
    @Transactional(readOnly = true)
    public StoreStaffDto getById(UUID id) { return mapper.toDto(requireAssignment(id)); }

    /** Lấy danh sách phân công theo cửa hàng, nhân viên, vai trò và trạng thái. */
    @Transactional(readOnly = true)
    public Page<StoreStaffDto> getList(
            UUID storeId, UUID userId, String staffRole, Boolean active, Pageable pageable) {
        validateSort(pageable);
        return repository.search(storeId, userId, normalizeFilter(staffRole), active, pageable).map(mapper::toDto);
    }

    /** Cập nhật phân công nhân viên tại cửa hàng. */
    @Transactional
    public StoreStaffDto update(UUID id, StoreStaffDto request) {
        StoreStaff entity = requireAssignment(id);
        validateRequest(request, id);
        mapper.updateEntity(request, entity);
        return mapper.toDto(repository.save(entity));
    }

    /** Kết thúc phân công nhân viên tại cửa hàng. */
    @Transactional
    public StoreStaffDto deactivate(UUID id) {
        StoreStaff entity = requireAssignment(id);
        entity.setActive(Boolean.FALSE);
        if (entity.getEndDate() == null) entity.setEndDate(java.time.LocalDate.now());
        return mapper.toDto(repository.save(entity));
    }

    /** Xóa một phân công nhân viên tại cửa hàng. */
    @Transactional
    public void delete(UUID id) { repository.delete(requireAssignment(id)); }

    private StoreStaff requireAssignment(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Phân công cửa hàng không tồn tại"));
    }

    private void validateRequest(StoreStaffDto request, UUID excludedId) {
        if (!storeRepository.existsById(request.getStoreId()))
            throw BusinessException.notFound("Cửa hàng không tồn tại");
        if (!userRepository.existsById(request.getUserId()))
            throw BusinessException.notFound("Nhân viên không tồn tại");
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate()))
            throw BusinessException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        if (!Boolean.FALSE.equals(request.getActive())) {
            boolean duplicate = excludedId == null
                    ? repository.existsByUserIdAndStoreIdAndActiveTrue(request.getUserId(), request.getStoreId())
                    : repository.existsByUserIdAndStoreIdAndActiveTrueAndIdNot(
                            request.getUserId(), request.getStoreId(), excludedId);
            if (duplicate) throw BusinessException.conflict("Nhân viên đã có phân công đang hoạt động tại cửa hàng");
        }
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp phân công cửa hàng không hợp lệ");
    }
    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
