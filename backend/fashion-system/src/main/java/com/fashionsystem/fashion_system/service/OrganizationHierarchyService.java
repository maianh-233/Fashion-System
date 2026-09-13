package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.Position;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.exception.HierarchyConfirmationRequiredException;
import com.fashionsystem.fashion_system.dto.hierarchy.HierarchyImpactResponse;
import com.fashionsystem.fashion_system.repository.PositionRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationHierarchyService {
    private final UserRepository userRepository;
    private final PositionRepository positionRepository;

    public record HierarchyImpact(Set<UUID> invalidSubordinateIds, Set<UUID> affectedEmployeeIds) {
        public HierarchyImpact {
            invalidSubordinateIds = Set.copyOf(invalidSubordinateIds);
            affectedEmployeeIds = Set.copyOf(affectedEmployeeIds);
        }
        public boolean isEmpty() { return invalidSubordinateIds.isEmpty(); }
        public HierarchyImpactResponse toResponse() {
            return new HierarchyImpactResponse(invalidSubordinateIds.size(), affectedEmployeeIds.size(),
                    "Thay đổi sẽ gỡ " + invalidSubordinateIds.size() + " quan hệ quản lý không hợp lệ, ảnh hưởng đến "
                            + affectedEmployeeIds.size() + " nhân viên.");
        }
    }

    /** Evaluate before changing the employee's position; never mutate the proposed or persisted entities. */
    @Transactional(readOnly = true)
    public HierarchyImpact analyzeEmployeeChange(User employee, Position proposedPosition) {
        if (employee == null || employee.getDeletedAt() != null || sameStructure(positionOf(employee), proposedPosition)) {
            return new HierarchyImpact(Set.of(), Set.of());
        }
        Map<UUID, User> relations = new LinkedHashMap<>();
        collectRelations(employee, relations);
        return analyze(relations, user -> employee.getId().equals(user.getId()) ? proposedPosition : positionOf(user));
    }

    /** Every holder sees the proposed department and level on both sides of every incident relation. */
    @Transactional(readOnly = true)
    public HierarchyImpact analyzePositionChange(Position position, UUID proposedDepartmentId, int proposedLevel) {
        Position proposed = Position.builder().id(position.getId()).departmentId(proposedDepartmentId)
                .hierarchyLevel(proposedLevel).build();
        if (sameStructure(position, proposed)) return new HierarchyImpact(Set.of(), Set.of());
        Map<UUID, User> relations = new LinkedHashMap<>();
        for (User holder : userRepository.findAllByPositionIdAndDeletedAtIsNull(position.getId())) {
            collectRelations(holder, relations);
        }
        return analyze(relations, user -> position.getId().equals(user.getPositionId()) ? proposed : positionOf(user));
    }

    @Transactional
    public void confirmOrClear(HierarchyImpact impact, Boolean resetInvalidRelations) {
        if (impact.isEmpty()) return;
        if (!Boolean.TRUE.equals(resetInvalidRelations)) throw new HierarchyConfirmationRequiredException(impact.toResponse());
        LocalDateTime now = LocalDateTime.now();
        for (UUID subordinateId : impact.invalidSubordinateIds()) {
            userRepository.findById(subordinateId).filter(user -> user.getDeletedAt() == null).ifPresent(user -> {
                user.setManagerId(null);
                user.setUpdatedAt(now);
                userRepository.save(user);
            });
        }
    }

    private boolean sameStructure(Position current, Position proposed) {
        if (current == null || proposed == null) return current == proposed;
        return Objects.equals(current.getDepartmentId(), proposed.getDepartmentId())
                && Objects.equals(current.getHierarchyLevel(), proposed.getHierarchyLevel());
    }

    private void collectRelations(User employee, Map<UUID, User> relations) {
        if (employee.getDeletedAt() != null) return;
        if (employee.getManagerId() != null) relations.put(employee.getId(), employee);
        for (User subordinate : userRepository.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(employee.getId())) {
            if (subordinate.getDeletedAt() == null && subordinate.getManagerId() != null) {
                relations.put(subordinate.getId(), subordinate);
            }
        }
    }

    private HierarchyImpact analyze(Map<UUID, User> relations, Function<User, Position> proposedPosition) {
        Set<UUID> invalid = new HashSet<>();
        Set<UUID> affected = new HashSet<>();
        for (User subordinate : relations.values()) {
            User manager = userRepository.findById(subordinate.getManagerId()).orElse(null);
            if (!isValidPair(manager, subordinate, manager == null ? null : proposedPosition.apply(manager),
                    proposedPosition.apply(subordinate))) {
                invalid.add(subordinate.getId());
                affected.add(subordinate.getId());
                affected.add(subordinate.getManagerId());
            }
        }
        return new HierarchyImpact(invalid, affected);
    }

    public void validateAssignment(User manager, User subordinate) {
        if (!isValidPair(manager, subordinate, positionOf(manager), positionOf(subordinate))) {
            throw BusinessException.conflict("Quan hệ quản lý không hợp lệ: nhân viên phải có vị trí cùng phòng ban, quản lý toàn thời gian có cấp bậc cao hơn và không tạo vòng lặp");
        }
    }

    private Position positionOf(User employee) {
        return employee == null || employee.getPositionId() == null ? null
                : positionRepository.findById(employee.getPositionId()).orElse(null);
    }

    private boolean isValidPair(User manager, User subordinate, Position managerPosition, Position subordinatePosition) {
        return manager != null && subordinate != null
                && manager.getDeletedAt() == null && subordinate.getDeletedAt() == null
                && manager.getId() != null && subordinate.getId() != null
                && !manager.getId().equals(subordinate.getId())
                && managerPosition != null && subordinatePosition != null
                && managerPosition.getDepartmentId() != null
                && Objects.equals(managerPosition.getDepartmentId(), subordinatePosition.getDepartmentId())
                && managerPosition.getHierarchyLevel() != null && subordinatePosition.getHierarchyLevel() != null
                && managerPosition.getHierarchyLevel() > subordinatePosition.getHierarchyLevel()
                && "FULL_TIME".equals(manager.getEmploymentType())
                && !hasCycle(manager, subordinate.getId());
    }

    private boolean hasCycle(User manager, UUID subordinateId) {
        Set<UUID> visited = new HashSet<>();
        User current = manager;
        while (current != null) {
            if (subordinateId.equals(current.getId()) || !visited.add(current.getId())) return true;
            current = current.getManagerId() == null ? null
                    : userRepository.findById(current.getManagerId()).orElse(null);
        }
        return false;
    }
}
