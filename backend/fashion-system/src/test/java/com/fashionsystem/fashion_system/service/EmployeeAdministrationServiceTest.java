package com.fashionsystem.fashion_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.dto.employee.CreateEmployeeRequest;
import com.fashionsystem.fashion_system.dto.employee.EmployeeScopeResponse;
import com.fashionsystem.fashion_system.dto.employee.EmployeeSummaryResponse;
import com.fashionsystem.fashion_system.dto.employee.UpdateEmployeeRequest;
import com.fashionsystem.fashion_system.dto.StoreDto;
import com.fashionsystem.fashion_system.entity.EmploymentType;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Position;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.exception.HierarchyConfirmationRequiredException;
import com.fashionsystem.fashion_system.mapper.StoreMapper;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EmployeeAdministrationServiceTest {
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock UserDepartmentRepository userDepartmentRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock PositionRepository positionRepository;
    @Mock StoreRepository storeRepository;
    @Mock StoreStaffRepository storeStaffRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock StoreMapper storeMapper;
    @Mock AuthAuditService authAuditService;
    @Mock AuditLogService auditLogService;
    @Mock EmployeeDataScopeService employeeDataScopeService;
    @Mock AuthorizationService authorizationService;
    private EmployeeAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeAdministrationService(userRepository, roleRepository, userRoleRepository,
                userDepartmentRepository, departmentRepository, positionRepository,
                storeRepository, storeStaffRepository, passwordEncoder, storeMapper, authAuditService, auditLogService,
                employeeDataScopeService, authorizationService,
                new OrganizationHierarchyService(userRepository, positionRepository));
    }

    @Test
    void storeListWithoutRequestedFilterUsesResolvedStoreForScopeAndFilter() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), "USER_VIEW");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("fullName"));
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, null)).thenReturn(storeId);
        when(userRepository.searchEmployees(storeId, storeId, "", "", "", pageable))
                .thenReturn(Page.empty(pageable));

        Page<?> result = service.getList(actorId, null, null, null, null, pageable);

        assertTrue(result.isEmpty());
        verify(userRepository).searchEmployees(storeId, storeId, "", "", "", pageable);
    }

    @Test
    void globalListWithStoreFilterUsesNullScopeAndRequestedStoreFilter() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.all("USER_VIEW");
        Pageable pageable = PageRequest.of(1, 25, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, storeId)).thenReturn(storeId);
        when(userRepository.searchEmployees(null, storeId, "", "", "", pageable))
                .thenReturn(Page.empty(pageable));

        service.getList(actorId, storeId, null, null, null, pageable);

        verify(userRepository).searchEmployees(null, storeId, "", "", "", pageable);
    }

    @Test
    void storeListRejectsAnotherStoreBeforeSearching() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), "USER_VIEW");
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, otherStoreId))
                .thenThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getList(actorId, otherStoreId, null, null, null, PageRequest.of(0, 10)));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).searchEmployees(any(), any(), any(), any(), any(), any());
    }

    @Test
    void listPreservesNormalizedFiltersAndPageableAlongsideScope() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.all("USER_VIEW");
        Pageable pageable = PageRequest.of(2, 7, Sort.by(Sort.Direction.ASC, "employeeCode"));
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, storeId)).thenReturn(storeId);
        when(userRepository.searchEmployees(null, storeId, "Lan", "MANAGER", "ACTIVE", pageable))
                .thenReturn(Page.empty(pageable));

        service.getList(actorId, storeId, " Lan ", " manager ", " active ", pageable);

        verify(userRepository).searchEmployees(null, storeId, "Lan", "MANAGER", "ACTIVE", pageable);
    }

    @Test
    void storeSummaryUsesDatabaseAggregateForResolvedStoreOnly() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), "USER_VIEW");
        EmployeeSummaryResponse expected = new EmployeeSummaryResponse(8, 6, 1, 2);
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, null)).thenReturn(storeId);
        when(userRepository.summarizeEmployees(eq(storeId), eq(storeId), any(LocalDateTime.class)))
                .thenReturn(expected);

        EmployeeSummaryResponse result = service.getSummary(actorId, null);

        assertEquals(expected, result);
        ArgumentCaptor<LocalDateTime> monthStart = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).summarizeEmployees(eq(storeId), eq(storeId), monthStart.capture());
        assertEquals(YearMonth.now().atDay(1).atStartOfDay(), monthStart.getValue());
        verify(userRepository, never()).searchEmployees(any(), any(), any(), any(), any(), any());
    }

    @Test
    void globalSummaryAcceptsNullAndRequestedStoreFilters() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.all("USER_VIEW");
        EmployeeSummaryResponse expected = new EmployeeSummaryResponse(4, 3, 0, 1);
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, null)).thenReturn(null);
        when(employeeDataScopeService.validateFilter(scope, storeId)).thenReturn(storeId);
        when(userRepository.summarizeEmployees(eq(null), eq(null), any(LocalDateTime.class))).thenReturn(expected);
        when(userRepository.summarizeEmployees(eq(null), eq(storeId), any(LocalDateTime.class))).thenReturn(expected);

        assertEquals(expected, service.getSummary(actorId, null));
        assertEquals(expected, service.getSummary(actorId, storeId));

        verify(userRepository).summarizeEmployees(eq(null), eq(null), any(LocalDateTime.class));
        verify(userRepository).summarizeEmployees(eq(null), eq(storeId), any(LocalDateTime.class));
    }

    @Test
    void storeAvailableStoresReturnsOnlyResolvedActiveStore() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Store store = activeStore(storeId, "A", "Store A");
        StoreDto dto = StoreDto.builder().id(storeId).code("A").name("Store A").active(true).build();
        EmployeeDataScope scope = EmployeeDataScope.store(store, "USER_VIEW");
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(storeRepository.findById(storeId)).thenReturn(java.util.Optional.of(store));
        when(storeMapper.toDto(store)).thenReturn(dto);

        assertEquals(List.of(dto), service.getAvailableStores(actorId));

        verify(storeRepository, never()).findAllByActiveTrueOrderByNameAsc();
    }

    @Test
    void globalAvailableStoresReturnsRepositorySortedActiveStores() {
        UUID actorId = UUID.randomUUID();
        Store storeA = activeStore(UUID.randomUUID(), "A", "Store A");
        Store storeB = activeStore(UUID.randomUUID(), "B", "Store B");
        StoreDto dtoA = StoreDto.builder().id(storeA.getId()).code("A").name("Store A").active(true).build();
        StoreDto dtoB = StoreDto.builder().id(storeB.getId()).code("B").name("Store B").active(true).build();
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW"))
                .thenReturn(EmployeeDataScope.all("USER_VIEW"));
        when(storeRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(storeA, storeB));
        when(storeMapper.toDto(storeA)).thenReturn(dtoA);
        when(storeMapper.toDto(storeB)).thenReturn(dtoB);

        assertEquals(List.of(dtoA, dtoB), service.getAvailableStores(actorId));
    }

    @Test
    void getScopeMapsGlobalScopeToAllWithNullStoreMetadata() {
        UUID actorId = UUID.randomUUID();
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW"))
                .thenReturn(EmployeeDataScope.all("USER_VIEW"));

        EmployeeScopeResponse result = service.getScope(actorId);

        assertEquals("ALL", result.scope());
        assertNull(result.storeId());
        assertNull(result.storeCode());
        assertNull(result.storeName());
    }

    @Test
    void getScopeMapsStoreScopeToExactStoreMetadata() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW"))
                .thenReturn(EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), "USER_VIEW"));

        EmployeeScopeResponse result = service.getScope(actorId);

        assertEquals("STORE", result.scope());
        assertEquals(storeId, result.storeId());
        assertEquals("A", result.storeCode());
        assertEquals("Store A", result.storeName());
    }

    @Test
    void storeActorCreateWithoutStoreUsesResolvedStore() {
        stubSuccessfulPersistence();
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), "USER_CREATE");
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").build();
        when(employeeDataScopeService.resolve(actorId, "USER_CREATE")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, null)).thenReturn(storeId);
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));
        when(departmentRepository.findById(departmentId)).thenReturn(java.util.Optional.of(
                Department.builder().id(departmentId).active(true).build()));
        when(positionRepository.findById(positionId)).thenReturn(java.util.Optional.of(
                Position.builder().id(positionId).departmentId(departmentId).active(true).name("Bán hàng").build()));
        when(storeRepository.findById(storeId)).thenReturn(java.util.Optional.of(activeStore(storeId, "A", "Store A")));

        service.create(actorId, new CreateEmployeeRequest(null, "staff.a@example.com", "Nhân viên A",
                "0912345678", null, EmploymentType.FULL_TIME, departmentId, positionId,
                Set.of("STAFF"), null));

        ArgumentCaptor<StoreStaff> assignment = ArgumentCaptor.forClass(StoreStaff.class);
        verify(storeStaffRepository).save(assignment.capture());
        assertEquals(storeId, assignment.getValue().getStoreId());
    }

    @Test
    void storeActorCreateForAnotherStoreIsForbiddenBeforeSave() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), "USER_CREATE");
        when(employeeDataScopeService.resolve(actorId, "USER_CREATE")).thenReturn(scope);
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(
                Role.builder().id(UUID.randomUUID()).code("STAFF").build()));
        when(employeeDataScopeService.validateFilter(scope, otherStoreId))
                .thenThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"));

        assertThrows(BusinessException.class, () -> service.create(actorId,
                new CreateEmployeeRequest(null, "staff.b@example.com", "Nhân viên B", "0912345678", null,
                        EmploymentType.FULL_TIME, UUID.randomUUID(), UUID.randomUUID(),
                        Set.of("STAFF"), otherStoreId)));

        verify(userRepository, never()).save(any());
    }

    @Test
    void crossStoreTargetIsDeniedForEveryReadAndMutationBeforePersistence() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User employee = User.builder().id(employeeId).employmentType("FULL_TIME").build();
        EmployeeDataScope readScope = EmployeeDataScope.store(
                activeStore(UUID.randomUUID(), "A", "Store A"), "USER_VIEW");
        EmployeeDataScope updateScope = new EmployeeDataScope(readScope.kind(), readScope.storeId(),
                readScope.storeCode(), readScope.storeName(), "USER_UPDATE");
        EmployeeDataScope deleteScope = new EmployeeDataScope(readScope.kind(), readScope.storeId(),
                readScope.storeCode(), readScope.storeName(), "USER_DELETE");
        when(userRepository.findById(employeeId)).thenReturn(java.util.Optional.of(employee));
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(readScope);
        when(employeeDataScopeService.resolve(actorId, "USER_UPDATE")).thenReturn(updateScope);
        when(employeeDataScopeService.resolve(actorId, "USER_DELETE")).thenReturn(deleteScope);
        doThrow(BusinessException.forbidden("cross store"))
                .when(employeeDataScopeService).requireTarget(any(), eq(employeeId));

        assertThrows(BusinessException.class, () -> service.getById(actorId, employeeId));
        assertThrows(BusinessException.class, () -> service.update(actorId, employeeId, null));
        assertThrows(BusinessException.class, () -> service.setLocked(actorId, employeeId, true));
        assertThrows(BusinessException.class, () -> service.restore(actorId, employeeId));
        assertThrows(BusinessException.class, () -> service.delete(actorId, employeeId));

        verify(userRepository, never()).save(any());
    }

    @Test
    void storeActorCannotMoveEmployeeToAnotherStore() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), "USER_UPDATE");
        User employee = User.builder().id(employeeId).username("employee.a").employeeCode("NV-A")
                .email("a@example.com").phone("0912345678").employmentType("FULL_TIME").build();
        StoreStaff assignment = StoreStaff.builder().userId(employeeId).storeId(storeId).active(true)
                .createdAt(LocalDateTime.now()).build();
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").build();
        when(userRepository.findById(employeeId)).thenReturn(java.util.Optional.of(employee));
        when(employeeDataScopeService.resolve(actorId, "USER_UPDATE")).thenReturn(scope);
        when(roleRepository.findCodesByUserId(employeeId)).thenReturn(List.of("STAFF"));
        when(storeStaffRepository.findAllByUserIdOrderByCreatedAtDesc(employeeId)).thenReturn(List.of(assignment));
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(employeeId)).thenReturn(List.of(assignment));
        when(storeRepository.findById(storeId)).thenReturn(java.util.Optional.of(activeStore(storeId, "A", "Store A")));
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));
        when(employeeDataScopeService.validateFilter(scope, otherStoreId))
                .thenThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"));

        UpdateEmployeeRequest request = new UpdateEmployeeRequest("employee.a", null, "Nhân viên A",
                "a@example.com", "0912345678", null, EmploymentType.FULL_TIME,
                departmentId, positionId, LocalDate.now(), "ACTIVE", true, false,
                Set.of("STAFF"), otherStoreId, null);

        assertThrows(BusinessException.class, () -> service.update(actorId, employeeId, request));
        verify(userRepository, never()).save(any());
    }

    private Store activeStore(UUID id, String code, String name) {
        return Store.builder().id(id).code(code).name(name).active(true).build();
    }

    private void stubSuccessfulPersistence() {
        when(userRepository.existsByEmployeeCode(any())).thenReturn(false);
        when(userRepository.findRegistrationConflicts(any(), any(), any(), any())).thenReturn(List.of());
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) user.setId(UUID.randomUUID());
            return user;
        });
        when(storeStaffRepository.findAllByUserIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");
    }

    @Test
    void createStaffGeneratesProtectedCredentialsAndAssignment() {
        stubSuccessfulPersistence();
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").name("Nhân viên").build();
        Department department = Department.builder().id(departmentId).active(true).build();
        Position position = Position.builder().id(positionId).departmentId(departmentId).name("Nhân viên bán hàng").active(true).build();
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));
        when(roleRepository.findCodesByUserId(any())).thenReturn(List.of("STAFF"));
        when(employeeDataScopeService.resolve(actorId, "USER_CREATE"))
                .thenReturn(EmployeeDataScope.all("USER_CREATE"));
        when(employeeDataScopeService.validateFilter(any(), eq(storeId))).thenReturn(storeId);
        when(storeRepository.findById(storeId)).thenReturn(java.util.Optional.of(activeStore(storeId, "A", "Store A")));
        when(departmentRepository.findById(departmentId)).thenReturn(java.util.Optional.of(department));
        when(positionRepository.findById(positionId)).thenReturn(java.util.Optional.of(position));

        var response = service.create(actorId, new CreateEmployeeRequest(
                null, "staff@example.com", " Nguyễn Văn An ", "0912345678", "Nhân viên bán hàng",
                EmploymentType.FULL_TIME, departmentId, positionId, Set.of("STAFF"), storeId));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertTrue(saved.getEmployeeCode().matches("NV\\d{8}[A-F0-9]{8}"));
        assertEquals(saved.getEmployeeCode().toLowerCase(), saved.getUsername());
        assertEquals("Nguyễn Văn An", saved.getFullName());
        assertEquals(LocalDate.now(), saved.getHireDate());
        assertEquals("FULL_TIME", saved.getEmploymentType());
        assertEquals(positionId, saved.getPositionId());
        assertEquals("Nhân viên bán hàng", saved.getJobTitle());
        verify(passwordEncoder).encode("Nguyễn Văn An");
        verify(storeStaffRepository).save(any(StoreStaff.class));
        verify(authAuditService).recordTransactional(actorId, AuthAuditService.EMPLOYEE_CREATED,
                "Tạo nhân viên " + saved.getEmployeeCode() + " (" + saved.getUsername() + ")");
        assertNotNull(response.employee().employeeCode());
        assertEquals("Nguyễn Văn An", response.temporaryPassword());
    }

    @Test
    void createAdminDoesNotApplyJobTitleOrStore() {
        stubSuccessfulPersistence();
        UUID actorId = UUID.randomUUID();
        Role admin = Role.builder().id(UUID.randomUUID()).code("ADMIN").name("Quản trị viên").build();
        when(employeeDataScopeService.resolve(actorId, "USER_CREATE"))
                .thenReturn(EmployeeDataScope.all("USER_CREATE"));
        when(roleRepository.findAllByCodeIn(Set.of("ADMIN"))).thenReturn(List.of(admin));
        when(roleRepository.findCodesByUserId(any())).thenReturn(List.of("ADMIN"));
        when(authorizationService.hasPermission(actorId, "USER_CREATE_ADMIN")).thenReturn(true);

        service.create(actorId, new CreateEmployeeRequest(
                "new.admin", "admin@example.com", "Quản Trị Viên", "0987654321", "Không áp dụng",
                EmploymentType.FULL_TIME, null, null, Set.of("ADMIN"), UUID.randomUUID()));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getJobTitle());
        verify(storeRepository, never()).existsById(any());
        verify(storeStaffRepository, never()).save(any(StoreStaff.class));
    }

    @Test
    void createStaffRequiresDepartmentBeforeSaving() {
        UUID actorId = UUID.randomUUID();
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").name("Nhân viên").build();
        when(employeeDataScopeService.resolve(actorId, "USER_CREATE"))
                .thenReturn(EmployeeDataScope.all("USER_CREATE"));
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));

        assertThrows(BusinessException.class, () -> service.create(actorId,
                new CreateEmployeeRequest(null, "staff@example.com", "Nguyễn Văn An", "0912345678", null,
                        EmploymentType.FULL_TIME, null, null, Set.of("STAFF"), UUID.randomUUID())));
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignSubordinateByEmailForFullTimeManager() {
        UUID actorId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID subordinateId = UUID.randomUUID();
        User manager = User.builder().id(managerId).employeeCode("NV-MANAGER")
                .employmentType("FULL_TIME").fullName("Quản lý").build();
        User subordinate = User.builder().id(subordinateId).employeeCode("NV-STAFF")
                .email("staff@example.com").fullName("Nhân viên").active(true).locked(false).build();
        when(userRepository.findById(managerId)).thenReturn(java.util.Optional.of(manager));
        when(employeeDataScopeService.resolve(actorId, "USER_UPDATE"))
                .thenReturn(EmployeeDataScope.all("USER_UPDATE"));
        when(userRepository.findByEmail("staff@example.com")).thenReturn(java.util.Optional.of(subordinate));
        when(userRepository.save(subordinate)).thenReturn(subordinate);
        when(roleRepository.findCodesByUserId(subordinateId)).thenReturn(List.of("STAFF"));
        when(storeStaffRepository.findAllByUserIdOrderByCreatedAtDesc(subordinateId)).thenReturn(List.of());
        UUID departmentId = UUID.randomUUID();
        Position managerPosition = Position.builder().id(UUID.randomUUID()).departmentId(departmentId)
                .hierarchyLevel(3).active(true).build();
        Position staffPosition = Position.builder().id(UUID.randomUUID()).departmentId(departmentId)
                .hierarchyLevel(1).active(true).build();
        manager.setPositionId(managerPosition.getId());
        subordinate.setPositionId(staffPosition.getId());
        lenient().when(positionRepository.findById(managerPosition.getId())).thenReturn(Optional.of(managerPosition));
        when(positionRepository.findById(staffPosition.getId())).thenReturn(Optional.of(staffPosition));

        service.assignSubordinate(actorId, managerId, " STAFF@example.com ");

        assertEquals(managerId, subordinate.getManagerId());
        verify(authAuditService).recordTransactional(actorId, AuthAuditService.SUBORDINATE_ASSIGNED,
                "Gán NV-STAFF dưới quyền NV-MANAGER");
    }

    @Test
    void nonFullTimeEmployeeCannotReceiveSubordinates() {
        UUID actorId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        User manager = User.builder().id(managerId).employmentType("PART_TIME").build();
        when(userRepository.findById(managerId)).thenReturn(java.util.Optional.of(manager));
        when(employeeDataScopeService.resolve(actorId, "USER_UPDATE"))
                .thenReturn(EmployeeDataScope.all("USER_UPDATE"));

        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(actorId, managerId, "staff@example.com"));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateRejectsDuplicateUsername() {
        UUID actorId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        User employee = User.builder().id(employeeId).username("old.username").employeeCode("NV001")
                .email("old@example.com").phone("0912345678").employmentType("FULL_TIME").build();
        User conflict = User.builder().id(UUID.randomUUID()).username("taken.username").build();
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").name("Nhân viên").build();
        when(userRepository.findById(employeeId)).thenReturn(java.util.Optional.of(employee));
        when(employeeDataScopeService.resolve(actorId, "USER_UPDATE"))
                .thenReturn(EmployeeDataScope.all("USER_UPDATE"));
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));
        when(storeRepository.findById(storeId)).thenReturn(java.util.Optional.of(activeStore(storeId, "A", "Store A")));
        when(departmentRepository.findById(departmentId)).thenReturn(java.util.Optional.of(
                Department.builder().id(departmentId).active(true).build()));
        when(positionRepository.findById(positionId)).thenReturn(java.util.Optional.of(
                Position.builder().id(positionId).departmentId(departmentId).active(true).build()));
        when(userRepository.findRegistrationConflicts("taken.username", "new@example.com", "NV001", "0987654321"))
                .thenReturn(List.of(conflict));

        var request = new UpdateEmployeeRequest("taken.username", null, "Nhân viên", "new@example.com",
                "0987654321", "Bán hàng", EmploymentType.FULL_TIME, departmentId, positionId,
                LocalDate.now(), "ACTIVE", true, false,
                Set.of("STAFF"), storeId, null);

        assertThrows(BusinessException.class,
                () -> service.update(actorId, employeeId, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejectsPositionFromAnotherDepartment() {
        UUID actorId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID otherDepartmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").name("Nhân viên").build();
        when(employeeDataScopeService.resolve(actorId, "USER_CREATE"))
                .thenReturn(EmployeeDataScope.all("USER_CREATE"));
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));
        when(departmentRepository.findById(departmentId)).thenReturn(java.util.Optional.of(
                Department.builder().id(departmentId).active(true).build()));
        when(positionRepository.findById(positionId)).thenReturn(java.util.Optional.of(
                Position.builder().id(positionId).departmentId(otherDepartmentId).active(true).build()));

        assertThrows(BusinessException.class, () -> service.create(actorId,
                new CreateEmployeeRequest(null, "staff@example.com", "Nguyễn Văn An", "0912345678", null,
                        EmploymentType.FULL_TIME, departmentId, positionId, Set.of("STAFF"), UUID.randomUUID())));
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void demotionToLowerOrEqualSubordinateLevelRequiresConfirmationBeforeAnyMutation(int level) {
        var fixture = hierarchyFixture();
        Position proposed = fixture.position(fixture.departmentId, level);
        var exception = assertThrows(HierarchyConfirmationRequiredException.class,
                () -> service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, null)));
        assertEquals(1L, exception.getBody().getProperties().get("affectedRelationCount"));
        assertEquals(2L, exception.getBody().getProperties().get("affectedEmployeeCount"));
        fixture.assertUnchanged();
        verifyNoEmployeeWrites();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void departmentMoveRequiresExplicitConfirmationForIncomingAndOutgoingLinks(Boolean confirmation) {
        var fixture = hierarchyFixture();
        Position proposed = fixture.position(UUID.randomUUID(), 3);
        var exception = assertThrows(HierarchyConfirmationRequiredException.class,
                () -> service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, confirmation)));
        assertEquals(2L, exception.getBody().getProperties().get("affectedRelationCount"));
        assertEquals(3L, exception.getBody().getProperties().get("affectedEmployeeCount"));
        fixture.assertUnchanged();
        verifyNoEmployeeWrites();
    }

    @Test
    void confirmedDepartmentMoveClearsBothDirectionsBeforeOrganizationMutationAndAuditsUpdate() {
        var fixture = hierarchyFixture();
        Position proposed = fixture.position(UUID.randomUUID(), 3);
        doAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            if (saved == fixture.child) assertEquals(fixture.originalPosition.getId(), fixture.employee.getPositionId());
            return saved;
        }).when(userRepository).save(any());
        var response = service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, true));
        assertNull(fixture.employee.getManagerId());
        assertNull(fixture.child.getManagerId());
        assertEquals(proposed.getId(), response.positionId());
        assertEquals(proposed.getName(), fixture.employee.getJobTitle());
        ArgumentCaptor<com.fashionsystem.fashion_system.entity.UserDepartment> membership =
                ArgumentCaptor.forClass(com.fashionsystem.fashion_system.entity.UserDepartment.class);
        verify(userDepartmentRepository).save(membership.capture());
        assertEquals(proposed.getDepartmentId(), membership.getValue().getDepartmentId());
        verify(authAuditService).recordTransactional(eq(fixture.actorId), eq(AuthAuditService.EMPLOYEE_UPDATED), any());
        verify(auditLogService).record(eq("UPDATE"), eq("EMPLOYEE"), eq(fixture.employee.getId()), any(), any());
    }

    @Test
    void confirmedDemotionRetainsValidIncomingAndUnrelatedRelations() {
        var fixture = hierarchyFixture();
        Position proposed = fixture.position(fixture.departmentId, 2);
        service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, true));
        assertEquals(fixture.boss.getId(), fixture.employee.getManagerId());
        assertNull(fixture.child.getManagerId());
        verify(userRepository, never()).save(fixture.boss);
    }

    @Test
    void promotionToManagerLevelRequiresConfirmationForIncomingLink() {
        var fixture = hierarchyFixture();
        Position proposed = fixture.position(fixture.departmentId, 4);
        assertThrows(HierarchyConfirmationRequiredException.class,
                () -> service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, false)));
        fixture.assertUnchanged();
        verifyNoEmployeeWrites();
    }

    @Test
    void nonstructuralEditDoesNotClearLinksEvenWhenResetIsTrue() {
        var fixture = hierarchyFixture();
        Position equivalent = fixture.position(fixture.departmentId, 3);
        service.update(fixture.actorId, fixture.employee.getId(), fixture.request(equivalent, true));
        assertEquals(fixture.boss.getId(), fixture.employee.getManagerId());
        assertEquals(fixture.employee.getId(), fixture.child.getManagerId());
        assertEquals(equivalent.getId(), fixture.employee.getPositionId());
        assertEquals("Updated employee", fixture.employee.getFullName());
        verify(userRepository, never()).save(fixture.child);
    }

    @Test
    void organizationValidationPrecedesConfirmedCleanup() {
        var fixture = hierarchyFixture();
        Position proposed = fixture.position(UUID.randomUUID(), 3);
        proposed.setActive(false);
        assertThrows(BusinessException.class,
                () -> service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, true)));
        fixture.assertUnchanged();
        verifyNoEmployeeWrites();
    }

    @Test
    void employeePreviewIsReadOnlyAndUpdateRecomputesImpactAfterPreview() {
        var fixture = hierarchyFixture();
        Position proposed = fixture.position(fixture.departmentId, 2);
        var preview = service.getHierarchyImpact(fixture.actorId, fixture.employee.getId(),
                proposed.getDepartmentId(), proposed.getId());
        assertEquals(1L, preview.affectedRelationCount());
        fixture.assertUnchanged();
        verifyNoEmployeeWrites();
        User additionalChild = fixture.user(fixture.originalPosition, "New child");
        additionalChild.setManagerId(fixture.employee.getId());
        var exception = assertThrows(HierarchyConfirmationRequiredException.class,
                () -> service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, null)));
        assertEquals(2L, exception.getBody().getProperties().get("affectedRelationCount"));
        verifyNoEmployeeWrites();
    }

    @Test
    void hierarchyPreviewAndCandidatesRejectOutOfScopeManagerBeforeReadingPositions() {
        UUID actorId = UUID.randomUUID();
        User manager = User.builder().id(UUID.randomUUID()).positionId(UUID.randomUUID()).build();
        var scope = EmployeeDataScope.store(activeStore(UUID.randomUUID(), "A", "Store A"), "USER_UPDATE");
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(employeeDataScopeService.resolve(actorId, "USER_UPDATE")).thenReturn(scope);
        doThrow(BusinessException.forbidden("cross store")).when(employeeDataScopeService).requireTarget(scope, manager.getId());
        assertThrows(BusinessException.class,
                () -> service.getHierarchyImpact(actorId, manager.getId(), UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(BusinessException.class, () -> service.getEligibleSubordinates(actorId, manager.getId()));
        verifyNoInteractions(positionRepository, departmentRepository);
        verify(userRepository, never()).findEligibleSubordinates(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void assignmentRejectsEqualOrHigherLevelSubordinate(int subordinateLevel) {
        var fixture = hierarchyFixture();
        fixture.child.setManagerId(null);
        fixture.positions.get(fixture.child.getPositionId()).setHierarchyLevel(subordinateLevel);
        fixture.originalPosition.setHierarchyLevel(2);
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.child.getEmail()));
        assertNull(fixture.child.getManagerId());
        verifyNoEmployeeWrites();
    }

    @Test
    void assignmentRejectsDifferentDepartment() {
        var fixture = hierarchyFixture();
        fixture.child.setManagerId(null);
        fixture.positions.get(fixture.child.getPositionId()).setDepartmentId(UUID.randomUUID());
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.child.getEmail()));
        assertNull(fixture.child.getManagerId());
        verifyNoEmployeeWrites();
    }

    @Test
    void assignmentExplicitlyRejectsInactiveSubordinate() {
        var fixture = hierarchyFixture();
        fixture.child.setManagerId(null);
        fixture.child.setActive(false);
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.child.getEmail()));
        assertNull(fixture.child.getManagerId());
        verifyNoEmployeeWrites();
    }

    @Test
    void assignmentExplicitlyRejectsLockedSubordinate() {
        var fixture = hierarchyFixture();
        fixture.child.setManagerId(null);
        fixture.child.setLocked(true);
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.child.getEmail()));
        assertNull(fixture.child.getManagerId());
        verifyNoEmployeeWrites();
    }

    @Test
    void assignmentRejectsExistingManagerSelfAndCycles() {
        var fixture = hierarchyFixture();
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.child.getEmail()));
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.employee.getEmail()));
        fixture.child.setManagerId(null);
        fixture.boss.setManagerId(fixture.child.getId());
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.child.getEmail()));
        assertNull(fixture.child.getManagerId());
        verifyNoEmployeeWrites();
    }

    @Test
    void assignmentRejectsOutOfScopeSubordinateBeforeMutationAndAudit() {
        var fixture = hierarchyFixture();
        fixture.child.setManagerId(null);
        doAnswer(call -> {
            if (fixture.child.getId().equals(call.getArgument(1))) throw BusinessException.forbidden("cross store");
            return null;
        }).when(employeeDataScopeService).requireTarget(any(), any());
        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(fixture.actorId, fixture.employee.getId(), fixture.child.getEmail()));
        assertNull(fixture.child.getManagerId());
        verifyNoEmployeeWrites();
    }

    @Test
    void candidateLookupRejectsMissingOrInactiveManagerPosition() {
        var fixture = hierarchyFixture();
        fixture.originalPosition.setActive(false);
        assertThrows(BusinessException.class, () -> service.getEligibleSubordinates(fixture.actorId, fixture.employee.getId()));
        fixture.employee.setPositionId(null);
        assertThrows(BusinessException.class, () -> service.getEligibleSubordinates(fixture.actorId, fixture.employee.getId()));
        verify(userRepository, never()).findEligibleSubordinates(any(), any(), any(), any());
    }

    @Test
    void candidateLookupRejectsNonFullTimeManager() {
        var fixture = hierarchyFixture();
        fixture.employee.setEmploymentType("PART_TIME");
        assertThrows(BusinessException.class, () -> service.getEligibleSubordinates(fixture.actorId, fixture.employee.getId()));
        verify(userRepository, never()).findEligibleSubordinates(any(), any(), any(), any());
    }

    @Test
    void departmentMoveWithNoRelationsDoesNotRequireConfirmation() {
        var fixture = hierarchyFixture();
        fixture.employee.setManagerId(null);
        fixture.child.setManagerId(null);
        Position proposed = fixture.position(UUID.randomUUID(), 1);
        service.update(fixture.actorId, fixture.employee.getId(), fixture.request(proposed, null));
        assertEquals(proposed.getId(), fixture.employee.getPositionId());
        verify(userRepository, never()).save(fixture.child);
    }

    private void verifyNoEmployeeWrites() {
        verify(userRepository, never()).save(any());
        verify(userRoleRepository, never()).deleteAllForUser(any());
        verify(userDepartmentRepository, never()).deleteAll(any(Iterable.class));
        verify(storeStaffRepository, never()).saveAll(any());
        verifyNoInteractions(authAuditService, auditLogService);
    }

    private HierarchyFixture hierarchyFixture() { return new HierarchyFixture(); }

    private class HierarchyFixture {
        final UUID actorId = UUID.randomUUID();
        final UUID departmentId = UUID.randomUUID();
        final Map<UUID, User> users = new HashMap<>();
        final Map<UUID, Position> positions = new HashMap<>();
        final Position originalPosition = position(departmentId, 3);
        final User boss = user(position(departmentId, 4), "Boss");
        final User employee = user(originalPosition, "Employee");
        final User child = user(position(departmentId, 2), "Child");

        HierarchyFixture() {
            employee.setManagerId(boss.getId());
            child.setManagerId(employee.getId());
            lenient().when(userRepository.findById(any())).thenAnswer(call -> Optional.ofNullable(users.get(call.getArgument(0))));
            lenient().when(userRepository.findByEmail(any())).thenAnswer(call -> users.values().stream()
                    .filter(user -> user.getEmail().equals(call.getArgument(0))).findFirst());
            lenient().when(userRepository.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(any()))
                    .thenAnswer(call -> users.values().stream().filter(user -> call.getArgument(0).equals(user.getManagerId()))
                            .filter(user -> user.getDeletedAt() == null).toList());
            lenient().when(positionRepository.findById(any())).thenAnswer(call -> Optional.ofNullable(positions.get(call.getArgument(0))));
            lenient().when(departmentRepository.findById(any())).thenAnswer(call -> Optional.of(
                    Department.builder().id(call.getArgument(0)).active(true).build()));
            lenient().when(employeeDataScopeService.resolve(actorId, "USER_UPDATE")).thenReturn(EmployeeDataScope.all("USER_UPDATE"));
            lenient().when(roleRepository.findAllByCodeIn(Set.of("STAFF")))
                    .thenReturn(List.of(Role.builder().id(UUID.randomUUID()).code("STAFF").build()));
            lenient().when(userRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        }

        Position position(UUID department, int level) {
            Position position = Position.builder().id(UUID.randomUUID()).departmentId(department)
                    .hierarchyLevel(level).name("Position " + level).active(true).build();
            positions.put(position.getId(), position);
            return position;
        }

        User user(Position position, String name) {
            User user = User.builder().id(UUID.randomUUID()).positionId(position.getId()).fullName(name)
                    .username(name.toLowerCase().replace(" ", ".")).email(UUID.randomUUID() + "@example.com")
                    .employeeCode("NV-" + name).phone("0912345678").employmentType("FULL_TIME")
                    .active(true).locked(false).build();
            users.put(user.getId(), user);
            return user;
        }

        UpdateEmployeeRequest request(Position proposed, Boolean confirmation) {
            return new UpdateEmployeeRequest(employee.getUsername(), null, "Updated employee", employee.getEmail(),
                    employee.getPhone(), "ignored client title", EmploymentType.FULL_TIME,
                    proposed.getDepartmentId(), proposed.getId(), LocalDate.now(), "ACTIVE", true, false,
                    Set.of("STAFF"), null, confirmation);
        }

        void assertUnchanged() {
            assertEquals(originalPosition.getId(), employee.getPositionId());
            assertEquals("Employee", employee.getFullName());
            assertEquals(boss.getId(), employee.getManagerId());
            assertEquals(employee.getId(), child.getManagerId());
        }
    }
}

@org.springframework.boot.test.context.SpringBootTest
@org.springframework.transaction.annotation.Transactional
class EmployeeEligibleSubordinatesQueryTest {
    @org.springframework.beans.factory.annotation.Autowired jakarta.persistence.EntityManager entityManager;
    @org.springframework.beans.factory.annotation.Autowired UserRepository users;
    @org.springframework.beans.factory.annotation.Autowired PositionRepository positions;

    @Test
    void eligibleCandidatesAreFilteredByPersistedOrganizationStatusAndActorStore() {
        Department department = persistDepartment();
        Position managerPosition = persistPosition(department, 3, true);
        Position low = persistPosition(department, 1, true);
        Position equal = persistPosition(department, 3, true);
        Position high = persistPosition(department, 4, true);
        Position otherDepartment = persistPosition(persistDepartment(), 1, true);
        Position inactivePosition = persistPosition(department, 1, false);
        Store scopedStore = persistStore();
        Store otherStore = persistStore();
        User manager = persistUser("Manager", managerPosition);
        User candidateB = persistUser("B Candidate", low);
        User candidateA = persistUser("A Candidate", low);
        User outside = persistUser("Outside", low);
        User inactiveMembership = persistUser("Inactive membership", low);
        User noMembership = persistUser("No membership", low);
        for (User user : List.of(manager, candidateA, candidateB)) membership(user, scopedStore, true);
        membership(outside, otherStore, true);
        membership(inactiveMembership, scopedStore, false);
        User inactive = persistUser("Inactive", low); inactive.setActive(false);
        User locked = persistUser("Locked", low); locked.setLocked(true);
        User assigned = persistUser("Assigned", low); assigned.setManagerId(manager.getId());
        User deleted = persistUser("Deleted", low); deleted.setDeletedAt(LocalDateTime.now());
        User noPosition = persistUser("No position", null);
        User wrongDepartment = persistUser("Other department", otherDepartment);
        User equalLevel = persistUser("Equal", equal);
        User higherLevel = persistUser("Higher", high);
        User inactiveJob = persistUser("Inactive position", inactivePosition);
        for (User user : List.of(inactive, locked, assigned, deleted, noPosition,
                wrongDepartment, equalLevel, higherLevel, inactiveJob)) membership(user, scopedStore, true);
        // A contradictory membership must not override the persisted position's department.
        entityManager.persist(com.fashionsystem.fashion_system.entity.UserDepartment.builder()
                .userId(wrongDepartment.getId()).departmentId(department.getId()).assignedAt(LocalDateTime.now()).build());
        entityManager.flush();
        UUID actorId = UUID.randomUUID();
        EmployeeDataScopeService scopes = org.mockito.Mockito.mock(EmployeeDataScopeService.class);
        EmployeeDataScope storeScope = EmployeeDataScope.store(scopedStore, "USER_UPDATE");
        when(scopes.resolve(actorId, "USER_UPDATE")).thenReturn(storeScope);
        var service = new EmployeeAdministrationService(users, null, null, null, null, positions,
                null, null, null, null, null, null, scopes, null, new OrganizationHierarchyService(users, positions));

        var scoped = service.getEligibleSubordinates(actorId, manager.getId());
        assertEquals(List.of(candidateA.getId(), candidateB.getId()), scoped.stream().map(value -> value.id()).toList());
        assertEquals(low.getId(), scoped.getFirst().positionId());
        assertEquals(low.getName(), scoped.getFirst().positionName());
        assertEquals(1, scoped.getFirst().hierarchyLevel());
        assertEquals(candidateA.getEmail(), scoped.getFirst().email());
        assertEquals(candidateA.getEmployeeCode(), scoped.getFirst().employeeCode());
        verify(scopes).requireTarget(storeScope, manager.getId());

        when(scopes.resolve(actorId, "USER_UPDATE")).thenReturn(EmployeeDataScope.all("USER_UPDATE"));
        var global = service.getEligibleSubordinates(actorId, manager.getId());
        assertEquals(Set.of(candidateA.getId(), candidateB.getId(), outside.getId(), inactiveMembership.getId(), noMembership.getId()),
                global.stream().map(value -> value.id()).collect(java.util.stream.Collectors.toSet()));

        // The manager must be excluded even if queried against a higher proposed level.
        assertTrue(users.findEligibleSubordinates(manager.getId(), department.getId(), 5, scopedStore.getId())
                .stream().noneMatch(value -> value.getId().equals(manager.getId())));
    }

    private Department persistDepartment() {
        var value = Department.builder().code("TEST-" + UUID.randomUUID()).name("Candidate query test")
                .active(true).createdAt(LocalDateTime.now()).build();
        entityManager.persist(value);
        return value;
    }
    private Position persistPosition(Department department, int level, boolean active) {
        var value = Position.builder().departmentId(department.getId()).code("TEST-" + UUID.randomUUID())
                .name("Position " + level).hierarchyLevel(level).active(active).createdAt(LocalDateTime.now()).build();
        entityManager.persist(value);
        return value;
    }
    private Store persistStore() {
        var value = Store.builder().code("T" + UUID.randomUUID().toString().substring(0, 8)).name("Candidate store")
                .phone("0912345678").latitude(java.math.BigDecimal.valueOf(Math.floorMod(UUID.randomUUID().hashCode(), 1000000), 6))
                .longitude(java.math.BigDecimal.valueOf(Math.floorMod(UUID.randomUUID().hashCode(), 1000000), 6))
                .active(true).createdAt(LocalDateTime.now()).build();
        entityManager.persist(value);
        return value;
    }
    private User persistUser(String name, Position position) {
        var value = User.builder().username("test-" + UUID.randomUUID()).fullName(name)
                .employeeCode("T" + UUID.randomUUID().toString().substring(0, 20))
                .email(UUID.randomUUID() + "@example.com").positionId(position == null ? null : position.getId())
                .employmentType("FULL_TIME").active(true).locked(false).createdAt(LocalDateTime.now()).build();
        entityManager.persist(value);
        return value;
    }
    private void membership(User user, Store store, boolean active) {
        entityManager.persist(StoreStaff.builder().userId(user.getId()).storeId(store.getId())
                .active(active).createdAt(LocalDateTime.now()).build());
    }
}
