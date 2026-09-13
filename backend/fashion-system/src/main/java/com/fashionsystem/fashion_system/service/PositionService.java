package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.dto.hierarchy.HierarchyImpactResponse;
import com.fashionsystem.fashion_system.dto.position.CreatePositionRequest;
import com.fashionsystem.fashion_system.dto.position.UpdatePositionRequest;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Position;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PositionMapper;
import com.fashionsystem.fashion_system.repository.DepartmentRepository;
import com.fashionsystem.fashion_system.repository.PositionRepository;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PositionService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "code", "name", "active", "createdAt", "updatedAt");
    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionMapper positionMapper;
    private final OrganizationHierarchyService hierarchyService;

    @Transactional
    public PositionDto create(CreatePositionRequest request) {
        Department department = requireActiveDepartment(request.departmentId());
        validateSalaryRange(request.minSalary(), request.maxSalary());
        ensureCodeAvailable(request.code());
        try {
            return positionMapper.toDto(
                    positionRepository.saveAndFlush(positionMapper.toEntity(request)), department);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("Mã vị trí đã tồn tại");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.POSITION_DETAIL, key = "#id")
    public PositionDto getById(UUID id) {
        Position position = requirePosition(id);
        return toDto(position);
    }

    @Transactional(readOnly = true)
    public Page<PositionDto> getList(String keyword, UUID departmentId, Boolean active, Pageable pageable) {
        validateSort(pageable);
        return positionRepository.search(keyword == null ? "" : keyword.trim(), departmentId, active, pageable)
                .map(this::toDto);
    }

    /** Lấy đầy đủ vị trí để nhúng vào màn hình chi tiết phòng ban. */
    @Transactional(readOnly = true)
    public List<PositionDto> getByDepartment(UUID departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> BusinessException.notFound("Phòng ban không tồn tại"));
        return positionRepository.findAllByDepartmentIdOrderByNameAsc(departmentId).stream()
                .map(position -> positionMapper.toDto(position, department))
                .toList();
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.POSITION_DETAIL, key = "#id")
    public PositionDto update(UUID id, UpdatePositionRequest request) {
        Position position = requirePosition(id);
        Department department = requireActiveDepartment(request.departmentId());
        validateSalaryRange(request.minSalary(), request.maxSalary());
        var impact = hierarchyService.analyzePositionChange(position, request.departmentId(), request.hierarchyLevel());
        if (!impact.isEmpty()) hierarchyService.confirmOrClear(impact, request.resetInvalidRelations());
        positionMapper.updateEntity(request, position);
        return positionMapper.toDto(positionRepository.save(position), department);
    }

    @Transactional(readOnly = true)
    public HierarchyImpactResponse getHierarchyImpact(UUID id, UUID departmentId, Integer hierarchyLevel) {
        Position position = requirePosition(id);
        requireActiveDepartment(departmentId);
        return hierarchyService.analyzePositionChange(position, departmentId, hierarchyLevel).toResponse();
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.POSITION_DETAIL, key = "#id")
    public void delete(UUID id) {
        Position position = requirePosition(id);
        try {
            positionRepository.delete(position);
            positionRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa vị trí đang được nhân viên sử dụng");
        }
    }

    private PositionDto toDto(Position position) {
        Department department = departmentRepository.findById(position.getDepartmentId()).orElse(null);
        return positionMapper.toDto(position, department);
    }
    private Position requirePosition(UUID id) {
        return positionRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Vị trí không tồn tại"));
    }
    private Department requireActiveDepartment(UUID id) {
        return departmentRepository.findById(id).filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> BusinessException.badRequest("Phòng ban không tồn tại hoặc đã ngừng hoạt động"));
    }
    private void ensureCodeAvailable(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (positionRepository.existsByCode(normalized)) {
            throw BusinessException.conflict("Mã vị trí đã tồn tại");
        }
    }
    private void validateSalaryRange(Long minSalary, Long maxSalary) {
        if (maxSalary < minSalary) {
            throw BusinessException.badRequest("Lương tối đa phải lớn hơn hoặc bằng lương tối thiểu");
        }
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !SORT_FIELDS.contains(order.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp vị trí không hợp lệ");
    }
}
