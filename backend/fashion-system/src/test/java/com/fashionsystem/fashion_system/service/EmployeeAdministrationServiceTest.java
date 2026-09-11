package com.fashionsystem.fashion_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private EmployeeAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeAdministrationService(userRepository, roleRepository, userRoleRepository,
                userDepartmentRepository, departmentRepository, positionRepository,
                storeRepository, storeStaffRepository, passwordEncoder, storeMapper, authAuditService, auditLogService,
                employeeDataScopeService);
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
    void legacyPrivilegeHintsCannotBroadenReadScope() {
        UUID actorId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Store store = activeStore(storeId, "A", "Store A");
        StoreDto storeDto = StoreDto.builder().id(storeId).code("A").name("Store A").active(true).build();
        EmployeeDataScope scope = EmployeeDataScope.store(store, "USER_VIEW");
        Pageable pageable = PageRequest.of(0, 10);
        EmployeeSummaryResponse summary = new EmployeeSummaryResponse(2, 2, 0, 1);
        when(employeeDataScopeService.resolve(actorId, "USER_VIEW")).thenReturn(scope);
        when(employeeDataScopeService.validateFilter(scope, null)).thenReturn(storeId);
        when(storeRepository.findById(storeId)).thenReturn(java.util.Optional.of(store));
        when(storeMapper.toDto(store)).thenReturn(storeDto);
        when(userRepository.searchEmployees(storeId, storeId, "", "", "", pageable))
                .thenReturn(Page.empty(pageable));
        when(userRepository.summarizeEmployees(eq(storeId), eq(storeId), any(LocalDateTime.class)))
                .thenReturn(summary);

        assertEquals(List.of(storeDto), service.getAvailableStores(actorId, true));
        assertTrue(service.getList(actorId, true, null, null, null, null, pageable).isEmpty());
        assertEquals(summary, service.getSummary(actorId, true, null));

        verify(userRepository).searchEmployees(storeId, storeId, "", "", "", pageable);
        verify(userRepository).summarizeEmployees(eq(storeId), eq(storeId), any(LocalDateTime.class));
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
        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(departmentRepository.findById(departmentId)).thenReturn(java.util.Optional.of(department));
        when(positionRepository.findById(positionId)).thenReturn(java.util.Optional.of(position));

        var response = service.create(actorId, true, true, new CreateEmployeeRequest(
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
        when(roleRepository.findAllByCodeIn(Set.of("ADMIN"))).thenReturn(List.of(admin));
        when(roleRepository.findCodesByUserId(any())).thenReturn(List.of("ADMIN"));

        service.create(actorId, true, true, new CreateEmployeeRequest(
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
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").name("Nhân viên").build();
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));

        assertThrows(BusinessException.class, () -> service.create(UUID.randomUUID(), true, true,
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
        when(userRepository.findByEmail("staff@example.com")).thenReturn(java.util.Optional.of(subordinate));
        when(userRepository.save(subordinate)).thenReturn(subordinate);
        when(roleRepository.findCodesByUserId(subordinateId)).thenReturn(List.of("STAFF"));
        when(storeStaffRepository.findAllByUserIdOrderByCreatedAtDesc(subordinateId)).thenReturn(List.of());

        service.assignSubordinate(actorId, true, managerId, " STAFF@example.com ");

        assertEquals(managerId, subordinate.getManagerId());
        verify(authAuditService).recordTransactional(actorId, AuthAuditService.SUBORDINATE_ASSIGNED,
                "Gán NV-STAFF dưới quyền NV-MANAGER");
    }

    @Test
    void nonFullTimeEmployeeCannotReceiveSubordinates() {
        UUID managerId = UUID.randomUUID();
        User manager = User.builder().id(managerId).employmentType("PART_TIME").build();
        when(userRepository.findById(managerId)).thenReturn(java.util.Optional.of(manager));

        assertThrows(BusinessException.class,
                () -> service.assignSubordinate(UUID.randomUUID(), true, managerId, "staff@example.com"));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateRejectsDuplicateUsername() {
        UUID employeeId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        User employee = User.builder().id(employeeId).username("old.username").employeeCode("NV001")
                .email("old@example.com").phone("0912345678").employmentType("FULL_TIME").build();
        User conflict = User.builder().id(UUID.randomUUID()).username("taken.username").build();
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").name("Nhân viên").build();
        when(userRepository.findById(employeeId)).thenReturn(java.util.Optional.of(employee));
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));
        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(departmentRepository.findById(departmentId)).thenReturn(java.util.Optional.of(
                Department.builder().id(departmentId).active(true).build()));
        when(positionRepository.findById(positionId)).thenReturn(java.util.Optional.of(
                Position.builder().id(positionId).departmentId(departmentId).active(true).build()));
        when(userRepository.findRegistrationConflicts("taken.username", "new@example.com", "NV001", "0987654321"))
                .thenReturn(List.of(conflict));

        var request = new UpdateEmployeeRequest("taken.username", null, "Nhân viên", "new@example.com",
                "0987654321", "Bán hàng", EmploymentType.FULL_TIME, departmentId, positionId,
                LocalDate.now(), "ACTIVE", true, false,
                Set.of("STAFF"), storeId);

        assertThrows(BusinessException.class,
                () -> service.update(UUID.randomUUID(), true, true, employeeId, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createRejectsPositionFromAnotherDepartment() {
        UUID departmentId = UUID.randomUUID();
        UUID otherDepartmentId = UUID.randomUUID();
        UUID positionId = UUID.randomUUID();
        Role staff = Role.builder().id(UUID.randomUUID()).code("STAFF").name("Nhân viên").build();
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(staff));
        when(departmentRepository.findById(departmentId)).thenReturn(java.util.Optional.of(
                Department.builder().id(departmentId).active(true).build()));
        when(positionRepository.findById(positionId)).thenReturn(java.util.Optional.of(
                Position.builder().id(positionId).departmentId(otherDepartmentId).active(true).build()));

        assertThrows(BusinessException.class, () -> service.create(UUID.randomUUID(), true, true,
                new CreateEmployeeRequest(null, "staff@example.com", "Nguyễn Văn An", "0912345678", null,
                        EmploymentType.FULL_TIME, departmentId, positionId, Set.of("STAFF"), UUID.randomUUID())));
        verify(userRepository, never()).save(any());
    }
}
