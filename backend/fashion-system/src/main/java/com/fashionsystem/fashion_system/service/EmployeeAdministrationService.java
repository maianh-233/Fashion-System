package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.employee.CreateEmployeeRequest;
import com.fashionsystem.fashion_system.dto.employee.CreateEmployeeResponse;
import com.fashionsystem.fashion_system.dto.employee.EmployeeResponse;
import com.fashionsystem.fashion_system.dto.employee.EmployeeScopeResponse;
import com.fashionsystem.fashion_system.dto.employee.EmployeeSummaryResponse;
import com.fashionsystem.fashion_system.dto.employee.UpdateEmployeeRequest;
import com.fashionsystem.fashion_system.dto.employee.EligibleSubordinateResponse;
import com.fashionsystem.fashion_system.dto.hierarchy.HierarchyImpactResponse;
import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.mapper.StoreMapper;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Position;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.entity.UserDepartment;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.DepartmentRepository;
import com.fashionsystem.fashion_system.repository.PositionRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.repository.UserRoleRepository;
import com.fashionsystem.fashion_system.repository.UserDepartmentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ nhân sự có giới hạn dữ liệu theo cửa hàng ở phía server. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("EMPLOYEE")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmployeeAdministrationService {
    private static final Set<String> PRIVILEGED_ROLES = Set.of("ADMIN", "SUPER_ADMIN");
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "employeeCode", "fullName", "email", "active", "locked", "createdAt", "updatedAt");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreMapper storeMapper;
    private final AuditLogService auditLogService;
    private final EmployeeDataScopeService employeeDataScopeService;
    private final AuthorizationService authorizationService;
    private final OrganizationHierarchyService organizationHierarchyService;

    @Transactional(readOnly = true)
    public HierarchyImpactResponse getHierarchyImpact(UUID actorId, UUID id, UUID departmentId, UUID positionId) {
        User employee = requireEmployee(id);
        requireTarget(actorId, "USER_UPDATE", employee.getId());
        Position proposedPosition = validateOrganization(departmentId, positionId, true);
        return organizationHierarchyService.analyzeEmployeeChange(employee, proposedPosition).toResponse();
    }

    @Transactional(readOnly = true)
    public List<EligibleSubordinateResponse> getEligibleSubordinates(UUID actorId, UUID managerId) {
        User manager = requireEmployee(managerId);
        EmployeeDataScope scope = requireTarget(actorId, "USER_UPDATE", manager.getId());
        if (!"FULL_TIME".equals(manager.getEmploymentType())) {
            throw BusinessException.conflict("Chỉ nhân viên toàn thời gian mới có thể có nhân viên dưới quyền");
        }
        Position managerPosition = manager.getPositionId() == null ? null
                : positionRepository.findById(manager.getPositionId()).orElse(null);
        if (managerPosition == null || !Boolean.TRUE.equals(managerPosition.getActive())) {
            throw BusinessException.badRequest("Vị trí không tồn tại hoặc đã ngừng hoạt động");
        }
        UUID scopeStoreId = scope.isGlobal() ? null : scope.storeId();
        return userRepository.findEligibleSubordinates(managerId, managerPosition.getDepartmentId(),
                managerPosition.getHierarchyLevel(), scopeStoreId).stream().map(subordinate -> {
                    Position position = positionRepository.findById(subordinate.getPositionId())
                            .orElseThrow(() -> BusinessException.conflict("Vị trí nhân viên không còn tồn tại"));
                    return new EligibleSubordinateResponse(subordinate.getId(), subordinate.getEmployeeCode(),
                            subordinate.getFullName(), subordinate.getEmail(), position.getId(),
                            position.getName(), position.getHierarchyLevel());
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<StoreDto> getAvailableStores(UUID actorId) {
        EmployeeDataScope scope = resolveReadScope(actorId);
        if (scope.isGlobal()) {
            return storeRepository.findAllByActiveTrueOrderByNameAsc().stream().map(storeMapper::toDto).toList();
        }
        Store store = storeRepository.findById(scope.storeId())
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> BusinessException.forbidden("Cửa hàng quản lý không còn hoạt động"));
        return List.of(storeMapper.toDto(store));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getList(UUID actorId, UUID storeId,
            String keyword, String roleCode, String status, Pageable pageable) {
        validateSort(pageable);
        EmployeeDataScope scope = resolveReadScope(actorId);
        UUID filterStoreId = employeeDataScopeService.validateFilter(scope, storeId);
        UUID scopeStoreId = scope.isGlobal() ? null : scope.storeId();
        return userRepository.searchEmployees(scopeStoreId, filterStoreId, normalizeFilter(keyword),
                normalizeCode(roleCode), normalizeCode(status), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID actorId, UUID id) {
        User employee = requireEmployee(id);
        requireTarget(actorId, "USER_VIEW", employee.getId());
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeSummaryResponse getSummary(UUID actorId, UUID storeId) {
        EmployeeDataScope scope = resolveReadScope(actorId);
        UUID filterStoreId = employeeDataScopeService.validateFilter(scope, storeId);
        UUID scopeStoreId = scope.isGlobal() ? null : scope.storeId();
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        return userRepository.summarizeEmployees(scopeStoreId, filterStoreId, monthStart);
    }

    @Transactional(readOnly = true)
    public EmployeeScopeResponse getScope(UUID actorId) {
        EmployeeDataScope scope = resolveReadScope(actorId);
        return new EmployeeScopeResponse(
                scope.kind().name(), scope.storeId(), scope.storeCode(), scope.storeName());
    }

    @Transactional
    public CreateEmployeeResponse create(UUID actorId, CreateEmployeeRequest request) {
        EmployeeDataScope scope = employeeDataScopeService.resolve(actorId, "USER_CREATE");
        Set<String> roleCodes = normalizeRoles(request.roleCodes());
        ensureRolesAllowed(actorId, scope, roleCodes);
        boolean privilegedTarget = hasPrivilegedTargetRole(roleCodes);
        boolean requiresOrganization = !privilegedTarget;
        UUID effectiveStoreId = normalizeCreateStore(scope, request.storeId(), privilegedTarget);
        Position position = validateOrganization(request.departmentId(), request.positionId(), requiresOrganization);

        String employeeCode = generateEmployeeCode();
        String username = request.username() == null || request.username().isBlank()
                ? employeeCode.toLowerCase(Locale.ROOT)
                : request.username().trim().toLowerCase(Locale.ROOT);
        if (username.length() < 3) throw BusinessException.badRequest("Tên đăng nhập phải có ít nhất 3 ký tự");
        ensureUnique(null, username, request.email(), employeeCode, request.phone());

        LocalDateTime now = LocalDateTime.now();
        User employee = userRepository.save(User.builder()
                .username(username)
                .email(request.email().trim().toLowerCase(Locale.ROOT))
                .phone(request.phone().trim())
                .passwordHash(passwordEncoder.encode(request.fullName().trim()))
                .employeeCode(employeeCode)
                .fullName(request.fullName().trim())
                .jobTitle(position == null ? null : position.getName())
                .positionId(position == null ? null : position.getId())
                .employmentType(request.employmentType().name())
                .employmentStatus("ACTIVE")
                .hireDate(LocalDate.now())
                .active(true).locked(false).failedLoginAttempts(0)
                .emailVerified(false).phoneVerified(false).lastPasswordChange(now).createdAt(now)
                .build());
        replaceRoles(employee.getId(), roleCodes, now);
        replaceDepartment(employee.getId(), requiresOrganization ? request.departmentId() : null, now);
        if (effectiveStoreId != null) {
            assignStore(employee.getId(), effectiveStoreId, primaryRole(roleCodes), now);
        }
        EmployeeResponse created = toResponse(employee);
        auditLogService.record("CREATE", "EMPLOYEE", employee.getId(), null, created);
        return new CreateEmployeeResponse(created, request.fullName().trim());
    }

    @Transactional
    public EmployeeResponse update(UUID actorId, UUID id, UpdateEmployeeRequest request) {
        User employee = requireEmployee(id);
        EmployeeDataScope scope = requireTarget(actorId, "USER_UPDATE", employee.getId());
        EmployeeResponse oldData = toResponse(employee);
        Set<String> roleCodes = normalizeRoles(request.roleCodes());
        ensureRolesAllowed(actorId, scope, roleCodes);
        boolean privilegedTarget = hasPrivilegedTargetRole(roleCodes);
        boolean requiresOrganization = !privilegedTarget;
        UUID effectiveStoreId = normalizeUpdateStore(scope, currentStoreId(id), request.storeId(), privilegedTarget);
        Position position = validateOrganization(request.departmentId(), request.positionId(), requiresOrganization);
        if (!"FULL_TIME".equals(request.employmentType().name())
                && userRepository.existsByManagerIdAndDeletedAtIsNull(id)) {
            throw BusinessException.conflict("Nhân viên đang có cấp dưới nên phải giữ loại hợp đồng toàn thời gian");
        }
        ensureUnique(id, request.username(), request.email(), employee.getEmployeeCode(), request.phone());

        var hierarchyImpact = organizationHierarchyService.analyzeEmployeeChange(employee, position);
        organizationHierarchyService.confirmOrClear(hierarchyImpact, request.resetInvalidRelations());

        employee.setUsername(request.username().trim().toLowerCase(Locale.ROOT));
        employee.setFullName(request.fullName().trim());
        employee.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        employee.setPhone(trimToNull(request.phone()));
        employee.setJobTitle(position == null ? null : position.getName());
        employee.setPositionId(position == null ? null : position.getId());
        employee.setEmploymentType(request.employmentType().name());
        employee.setHireDate(request.hireDate());
        employee.setEmploymentStatus(request.employmentStatus());
        employee.setActive(request.active());
        employee.setLocked(request.locked());
        if (!request.locked()) {
            employee.setLoginLockedUntil(null);
        }
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            employee.setLastPasswordChange(LocalDateTime.now());
            employee.setFailedLoginAttempts(0);
            employee.setLoginLockedUntil(null);
        }
        employee.setDeletedAt(null);
        employee.setUpdatedAt(LocalDateTime.now());
        userRepository.save(employee);
        replaceRoles(id, roleCodes, LocalDateTime.now());
        replaceDepartment(id, requiresOrganization ? request.departmentId() : null, LocalDateTime.now());
        replaceStore(id, effectiveStoreId, primaryRole(roleCodes));
        EmployeeResponse updated = toResponse(employee);
        auditLogService.record("UPDATE", "EMPLOYEE", id, oldData, updated);
        return updated;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getSubordinates(UUID actorId, UUID managerId) {
        User manager = requireEmployee(managerId);
        EmployeeDataScope scope = requireTarget(actorId, "USER_VIEW", manager.getId());
        return userRepository.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(managerId).stream()
                .peek(subordinate -> employeeDataScopeService.requireTarget(scope, subordinate.getId()))
                .map(this::toResponse).toList();
    }

    @Transactional
    public EmployeeResponse assignSubordinate(UUID actorId, UUID managerId, String email) {
        User manager = requireEmployee(managerId);
        EmployeeDataScope scope = requireTarget(actorId, "USER_UPDATE", manager.getId());
        if (!"FULL_TIME".equals(manager.getEmploymentType())) {
            throw BusinessException.conflict("Chỉ nhân viên toàn thời gian mới có thể có nhân viên dưới quyền");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User subordinate = userRepository.findByEmail(normalizedEmail)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy nhân viên với email này"));
        employeeDataScopeService.requireTarget(scope, subordinate.getId());
        if (!Boolean.TRUE.equals(subordinate.getActive()) || Boolean.TRUE.equals(subordinate.getLocked())) {
            throw BusinessException.conflict("Chỉ có thể thêm nhân viên đang hoạt động và không bị khóa");
        }
        if (managerId.equals(subordinate.getId())) {
            throw BusinessException.badRequest("Nhân viên không thể tự làm cấp dưới của chính mình");
        }
        if (subordinate.getManagerId() != null) {
            if (managerId.equals(subordinate.getManagerId())) {
                throw BusinessException.conflict("Nhân viên này đã thuộc quyền quản lý của người được chọn");
            }
            throw BusinessException.conflict("Nhân viên này đang thuộc quyền quản lý của người khác");
        }
        organizationHierarchyService.validateAssignment(manager, subordinate);

        subordinate.setManagerId(managerId);
        subordinate.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(subordinate);
        return toResponse(saved);
    }

    @Transactional
    public void removeSubordinate(UUID actorId, UUID managerId, UUID subordinateId) {
        User manager = requireEmployee(managerId);
        User subordinate = requireEmployee(subordinateId);
        EmployeeDataScope scope = requireTarget(actorId, "USER_UPDATE", manager.getId());
        employeeDataScopeService.requireTarget(scope, subordinate.getId());
        if (!managerId.equals(subordinate.getManagerId())) {
            throw BusinessException.notFound("Nhân viên không thuộc quyền quản lý của người được chọn");
        }
        subordinate.setManagerId(null);
        subordinate.setUpdatedAt(LocalDateTime.now());
        userRepository.save(subordinate);
    }

    @Transactional
    public EmployeeResponse setLocked(UUID actorId, UUID id, boolean locked) {
        User employee = requireEmployee(id);
        requireTarget(actorId, "USER_UPDATE", employee.getId());
        if (actorId.equals(id)) throw BusinessException.badRequest("Bạn không thể tự khóa tài khoản của mình");
        ensurePrivilegedTargetAllowed(actorId, id);
        employee.setLocked(locked);
        employee.setFailedLoginAttempts(locked ? employee.getFailedLoginAttempts() : 0);
        if (!locked) {
            employee.setLoginLockedUntil(null);
        }
        employee.setUpdatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(employee));
    }

    @Transactional
    public void delete(UUID actorId, UUID id) {
        User employee = requireEmployee(id);
        requireTarget(actorId, "USER_DELETE", employee.getId());
        if (actorId.equals(id)) throw BusinessException.badRequest("Bạn không thể tự xóa tài khoản của mình");
        ensurePrivilegedTargetAllowed(actorId, id);
        EmployeeResponse oldData = toResponse(employee);
        employee.setActive(false);
        employee.setDeletedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        userRepository.save(employee);
        auditLogService.record("DELETE", "EMPLOYEE", id, oldData, null);
    }

    @Transactional
    public EmployeeResponse restore(UUID actorId, UUID id) {
        User employee = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Nhân viên không tồn tại"));
        requireTarget(actorId, "USER_UPDATE", employee.getId());
        EmployeeResponse oldData = toResponse(employee);
        employee.setDeletedAt(null);
        employee.setActive(true);
        employee.setEmploymentStatus("ACTIVE");
        employee.setUpdatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(employee));
    }

    private EmployeeDataScope resolveReadScope(UUID actorId) {
        return employeeDataScopeService.resolve(actorId, "USER_VIEW");
    }

    private EmployeeResponse toResponse(User user) {
        List<String> roles = roleRepository.findCodesByUserId(user.getId());
        StoreStaff assignment = storeStaffRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive())).findFirst().orElse(null);
        Store store = assignment == null ? null : storeRepository.findById(assignment.getStoreId()).orElse(null);
        UserDepartment membership = userDepartmentRepository.findAllByUserId(user.getId()).stream().findFirst().orElse(null);
        Department department = membership == null ? null : departmentRepository.findById(membership.getDepartmentId()).orElse(null);
        Position position = user.getPositionId() == null ? null : positionRepository.findById(user.getPositionId()).orElse(null);
        User manager = user.getManagerId() == null ? null : userRepository.findById(user.getManagerId()).orElse(null);
        return new EmployeeResponse(user.getId(), user.getUsername(), user.getEmployeeCode(), user.getFullName(),
                user.getEmail(), user.getPhone(), user.getJobTitle(), user.getEmploymentType(),
                user.getEmploymentStatus(), user.getHireDate(), user.getActive(), user.getLocked(),
                user.getDeletedAt(), user.getCreatedAt(), roles,
                store == null ? null : store.getId(), store == null ? null : store.getCode(),
                store == null ? null : store.getName(),
                department == null ? null : department.getId(), department == null ? null : department.getCode(),
                department == null ? null : department.getName(),
                position == null ? null : position.getId(), position == null ? null : position.getCode(),
                position == null ? null : position.getName(),
                user.getManagerId(), manager == null ? null : manager.getFullName());
    }

    private User requireEmployee(UUID id) {
        return userRepository.findById(id).filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("Nhân viên không tồn tại"));
    }

    private EmployeeDataScope requireTarget(UUID actorId, String permissionCode, UUID targetUserId) {
        EmployeeDataScope scope = employeeDataScopeService.resolve(actorId, permissionCode);
        employeeDataScopeService.requireTarget(scope, targetUserId);
        return scope;
    }

    private Position validateOrganization(UUID departmentId, UUID positionId, boolean required) {
        if (!required) return null;
        if (departmentId == null) throw BusinessException.badRequest("Phòng ban là bắt buộc với vai trò nhân viên");
        if (positionId == null) throw BusinessException.badRequest("Vị trí là bắt buộc với vai trò nhân viên");
        Department department = departmentRepository.findById(departmentId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> BusinessException.badRequest("Phòng ban không tồn tại hoặc đã ngừng hoạt động"));
        Position position = positionRepository.findById(positionId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> BusinessException.badRequest("Vị trí không tồn tại hoặc đã ngừng hoạt động"));
        if (!department.getId().equals(position.getDepartmentId())) {
            throw BusinessException.badRequest("Vị trí không thuộc phòng ban đã chọn");
        }
        return position;
    }

    private void ensureRolesAllowed(UUID actorId, EmployeeDataScope scope, Set<String> roles) {
        List<Role> existing = roleRepository.findAllByCodeIn(roles);
        if (existing.size() != roles.size()) throw BusinessException.badRequest("Có vai trò không tồn tại");
        if (!scope.isGlobal() && hasPrivilegedTargetRole(roles))
            throw BusinessException.forbidden("Người dùng phạm vi cửa hàng không được cấp vai trò quản trị toàn hệ thống");
        if (roles.contains("ADMIN") && !authorizationService.hasPermission(actorId, "USER_CREATE_ADMIN"))
            throw BusinessException.forbidden("Bạn không có quyền cấp vai trò quản trị viên");
        if (roles.contains("SUPER_ADMIN")
                && !roleRepository.findCodesByUserId(actorId).contains("SUPER_ADMIN"))
            throw BusinessException.forbidden("Chỉ quản trị viên tối cao được cấp vai trò SUPER_ADMIN");
    }

    private UUID normalizeCreateStore(EmployeeDataScope scope, UUID requestedStoreId, boolean privilegedTarget) {
        if (privilegedTarget) return null;
        UUID effectiveStoreId = employeeDataScopeService.validateFilter(scope, requestedStoreId);
        validateActiveStore(effectiveStoreId);
        return effectiveStoreId;
    }

    private UUID normalizeUpdateStore(EmployeeDataScope scope, UUID existingStoreId,
            UUID requestedStoreId, boolean privilegedTarget) {
        if (!scope.isGlobal()) {
            if (privilegedTarget) {
                throw BusinessException.forbidden("Người dùng phạm vi cửa hàng không thể tạo nhân viên toàn hệ thống");
            }
            UUID effectiveStoreId = employeeDataScopeService.validateFilter(scope, requestedStoreId);
            if (!scope.storeId().equals(existingStoreId)) {
                throw BusinessException.forbidden("Bạn không được chuyển nhân viên khỏi cửa hàng hiện tại");
            }
            return effectiveStoreId;
        }
        if (privilegedTarget) return null;
        validateActiveStore(requestedStoreId);
        return requestedStoreId;
    }

    private void validateActiveStore(UUID storeId) {
        if (storeId == null) return;
        storeRepository.findById(storeId)
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .orElseThrow(() -> BusinessException.badRequest("Cửa hàng không tồn tại hoặc đã ngừng hoạt động"));
    }

    private UUID currentStoreId(UUID userId) {
        List<UUID> storeIds = storeStaffRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(StoreStaff::getStoreId).distinct().toList();
        if (storeIds.size() > 1) {
            throw BusinessException.conflict("Nhân viên đang được gán nhiều cửa hàng hoạt động");
        }
        return storeIds.isEmpty() ? null : storeIds.getFirst();
    }

    private void ensurePrivilegedTargetAllowed(UUID actorId, UUID targetId) {
        Set<String> targetRoles = Set.copyOf(roleRepository.findCodesByUserId(targetId));
        if (targetRoles.contains("ADMIN") && !authorizationService.hasPermission(actorId, "USER_CREATE_ADMIN")) {
            throw BusinessException.forbidden("Bạn không được quản lý tài khoản quản trị viên");
        }
        if (targetRoles.contains("SUPER_ADMIN")
                && !roleRepository.findCodesByUserId(actorId).contains("SUPER_ADMIN")) {
            throw BusinessException.forbidden("Chỉ quản trị viên tối cao được quản lý tài khoản SUPER_ADMIN");
        }
    }

    private void ensureUnique(UUID excludedId, String username, String email, String employeeCode, String phone) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedCode = employeeCode.trim().toUpperCase(Locale.ROOT);
        String normalizedPhone = trimToNull(phone);
        List<User> conflicts = userRepository.findRegistrationConflicts(
                normalizedUsername, normalizedEmail, normalizedCode, normalizedPhone).stream()
                .filter(user -> excludedId == null || !excludedId.equals(user.getId())).toList();
        if (conflicts.stream().anyMatch(user -> normalizedUsername.equals(user.getUsername()))) {
            throw BusinessException.conflict("Tên đăng nhập đã tồn tại");
        }
        if (conflicts.stream().anyMatch(user -> normalizedEmail.equals(user.getEmail()))) {
            throw BusinessException.conflict("Email đã tồn tại");
        }
        if (conflicts.stream().anyMatch(user -> normalizedCode.equals(user.getEmployeeCode()))) {
            throw BusinessException.conflict("Mã nhân viên đã tồn tại");
        }
        if (normalizedPhone != null && conflicts.stream().anyMatch(user -> normalizedPhone.equals(user.getPhone()))) {
            throw BusinessException.conflict("Số điện thoại đã tồn tại");
        }
    }

    private void replaceRoles(UUID userId, Set<String> roleCodes, LocalDateTime now) {
        userRoleRepository.deleteAllForUser(userId);
        List<Role> roles = roleRepository.findAllByCodeIn(roleCodes);
        userRoleRepository.saveAll(roles.stream().map(role -> UserRole.builder()
                .userId(userId).roleId(role.getId()).assignedAt(now).build()).toList());
    }

    private void replaceDepartment(UUID userId, UUID departmentId, LocalDateTime now) {
        userDepartmentRepository.deleteAll(userDepartmentRepository.findAllByUserId(userId));
        if (departmentId != null) {
            userDepartmentRepository.save(UserDepartment.builder()
                    .userId(userId).departmentId(departmentId).assignedAt(now).build());
        }
    }

    private void replaceStore(UUID userId, UUID storeId, String staffRole) {
        List<StoreStaff> assignments = storeStaffRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        assignments.forEach(item -> item.setActive(false));
        storeStaffRepository.saveAll(assignments);
        if (storeId != null) assignStore(userId, storeId, staffRole, LocalDateTime.now());
    }

    private void assignStore(UUID userId, UUID storeId, String staffRole, LocalDateTime now) {
        storeStaffRepository.save(StoreStaff.builder().userId(userId).storeId(storeId).staffRole(staffRole)
                .startDate(LocalDate.now()).active(true).createdAt(now).build());
    }

    private Set<String> normalizeRoles(Set<String> values) {
        return values.stream().map(this::normalizeCode).collect(Collectors.toSet());
    }

    private boolean hasPrivilegedTargetRole(Set<String> roles) {
        return roles.stream().anyMatch(PRIVILEGED_ROLES::contains);
    }

    private String generateEmployeeCode() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String code;
        do {
            code = "NV" + date + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 8).toUpperCase(Locale.ROOT);
        } while (userRepository.existsByEmployeeCode(code));
        return code;
    }

    private String primaryRole(Set<String> roles) {
        return roles.stream().sorted(Comparator.naturalOrder()).findFirst().orElse("STAFF");
    }

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(order -> !SORT_FIELDS.contains(order.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp nhân viên không hợp lệ");
    }

    private String normalizeFilter(String value) { return value == null ? "" : value.trim(); }
    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
