package com.fashionsystem.fashion_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.dto.position.CreatePositionRequest;
import com.fashionsystem.fashion_system.dto.position.UpdatePositionRequest;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Position;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.HierarchyConfirmationRequiredException;
import com.fashionsystem.fashion_system.repository.UserRepository;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PositionMapper;
import com.fashionsystem.fashion_system.repository.DepartmentRepository;
import com.fashionsystem.fashion_system.repository.PositionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {
    @Mock PositionRepository positionRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock UserRepository userRepository;
    private OrganizationHierarchyService hierarchyService;
    private PositionService service;

    @BeforeEach
    void setUp() {
        hierarchyService = org.mockito.Mockito.spy(new OrganizationHierarchyService(userRepository, positionRepository));
        service = new PositionService(positionRepository, departmentRepository, new PositionMapper(), hierarchyService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(booleans = false)
    void levelChangeRequiresExplicitConfirmationBeforeAnyMutation(Boolean confirmation) {
        var fixture = hierarchyFixture();
        var error = assertThrows(HierarchyConfirmationRequiredException.class,
                () -> service.update(fixture.position().getId(), change(fixture.position().getDepartmentId(), 2, confirmation)));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("HIERARCHY_CONFIRMATION_REQUIRED", error.getBody().getProperties().get("code"));
        assertEquals(1L, error.getBody().getProperties().get("affectedRelationCount"));
        assertEquals(2L, error.getBody().getProperties().get("affectedEmployeeCount"));
        assertEquals(3, fixture.position().getHierarchyLevel());
        assertEquals("Tên cũ", fixture.position().getName());
        assertEquals(fixture.manager().getId(), fixture.invalid().getManagerId());
        verify(positionRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void departmentChangeRequiresConfirmation() {
        var fixture = hierarchyFixture();
        UUID proposedDepartment = UUID.randomUUID();
        when(departmentRepository.findById(proposedDepartment)).thenReturn(Optional.of(activeDepartment(proposedDepartment)));
        assertThrows(HierarchyConfirmationRequiredException.class,
                () -> service.update(fixture.position().getId(), change(proposedDepartment, 3, false)));
        assertEquals(fixture.manager().getId(), fixture.invalid().getManagerId());
        verify(positionRepository, never()).save(any());
    }

    @Test
    void confirmedChangeClearsOnlyInvalidRelationsBeforeSavingPosition() {
        var fixture = hierarchyFixture();
        when(positionRepository.save(fixture.position())).thenAnswer(call -> {
            assertNull(fixture.invalid().getManagerId());
            assertEquals(fixture.manager().getId(), fixture.valid().getManagerId());
            return call.getArgument(0);
        });
        var result = service.update(fixture.position().getId(), change(fixture.position().getDepartmentId(), 2, true));
        assertEquals(2, result.getHierarchyLevel());
        assertNull(fixture.invalid().getManagerId());
        assertEquals(fixture.manager().getId(), fixture.valid().getManagerId());
        verify(userRepository).save(fixture.invalid());
        verify(userRepository, never()).save(fixture.valid());
    }

    @Test
    void nameSalaryAndActiveOnlyChangesNeverClearRelationsEvenWithConfirmation() {
        UUID departmentId = UUID.randomUUID();
        Position existing = position(departmentId, "LEAD", 3, 10L, 20L);
        when(positionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));
        when(positionRepository.save(existing)).thenReturn(existing);
        service.update(existing.getId(), new UpdatePositionRequest(departmentId, "New", "Changed", false, 3, 30L, 40L, true));
        assertEquals("New", existing.getName());
        assertEquals(false, existing.getActive());
        assertEquals(30L, existing.getMinSalary());
        verifyNoInteractions(userRepository);
        verify(hierarchyService, never()).confirmOrClear(any(), any());
    }

    @Test
    void updateRecomputesImpactAfterAnEarlierZeroImpactPreview() {
        var fixture = hierarchyFixture();
        when(userRepository.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(fixture.manager().getId()))
                .thenReturn(List.of(fixture.valid()));
        assertEquals(0L, service.getHierarchyImpact(fixture.position().getId(), fixture.position().getDepartmentId(), 2)
                .affectedRelationCount());
        when(userRepository.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(fixture.manager().getId()))
                .thenReturn(List.of(fixture.invalid(), fixture.valid()));
        assertThrows(HierarchyConfirmationRequiredException.class,
                () -> service.update(fixture.position().getId(), change(fixture.position().getDepartmentId(), 2, false)));
        assertEquals(1L, service.getHierarchyImpact(fixture.position().getId(), fixture.position().getDepartmentId(), 2)
                .affectedRelationCount());
        verify(positionRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void departmentAndLevelChangesWithZeroImpactNeedNoConfirmation() {
        UUID oldDepartment = UUID.randomUUID();
        UUID newDepartment = UUID.randomUUID();
        Position existing = position(oldDepartment, "LEAD", 3, 10L, 20L);
        when(positionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(newDepartment)).thenReturn(Optional.of(activeDepartment(newDepartment)));
        when(positionRepository.save(existing)).thenReturn(existing);
        var result = service.update(existing.getId(), change(newDepartment, 4, null));
        assertEquals(newDepartment, result.getDepartmentId());
        assertEquals(4, result.getHierarchyLevel());
        verify(userRepository, never()).save(any());
    }

    @Test
    void previewIsReadOnlyAndUncachedWhileUpdateHasOneOuterWriteTransaction() throws Exception {
        var preview = PositionService.class.getMethod("getHierarchyImpact", UUID.class, UUID.class, Integer.class);
        var update = PositionService.class.getMethod("update", UUID.class, UpdatePositionRequest.class);
        assertEquals(true, preview.getAnnotation(org.springframework.transaction.annotation.Transactional.class).readOnly());
        assertNull(preview.getAnnotation(Cacheable.class));
        assertNull(preview.getAnnotation(CachePut.class));
        assertEquals(false, update.getAnnotation(org.springframework.transaction.annotation.Transactional.class).readOnly());
        assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRED,
                update.getAnnotation(org.springframework.transaction.annotation.Transactional.class).propagation());
    }

    private UpdatePositionRequest change(UUID departmentId, int level, Boolean confirmation) {
        return new UpdatePositionRequest(departmentId, "Updated", null, true, level, 10L, 20L, confirmation);
    }

    private HierarchyFixture hierarchyFixture() {
        UUID departmentId = UUID.randomUUID();
        Position managerPosition = position(departmentId, "LEAD", 3, 10L, 20L);
        Position invalidPosition = position(departmentId, "MID", 2, 10L, 20L);
        Position validPosition = position(departmentId, "JUNIOR", 1, 10L, 20L);
        User manager = User.builder().id(UUID.randomUUID()).positionId(managerPosition.getId()).employmentType("FULL_TIME").build();
        User invalid = User.builder().id(UUID.randomUUID()).positionId(invalidPosition.getId()).managerId(manager.getId()).build();
        User valid = User.builder().id(UUID.randomUUID()).positionId(validPosition.getId()).managerId(manager.getId()).build();
        for (Position p : List.of(managerPosition, invalidPosition, validPosition))
            lenient().when(positionRepository.findById(p.getId())).thenReturn(Optional.of(p));
        lenient().when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));
        lenient().when(userRepository.findAllByPositionIdAndDeletedAtIsNull(managerPosition.getId())).thenReturn(List.of(manager));
        lenient().when(userRepository.findAllByManagerIdAndDeletedAtIsNullOrderByFullNameAsc(manager.getId())).thenReturn(List.of(invalid, valid));
        for (User u : List.of(manager, invalid, valid))
            lenient().when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        return new HierarchyFixture(managerPosition, manager, invalid, valid);
    }

    private record HierarchyFixture(Position position, User manager, User invalid, User valid) {}

    @Test
    void createNormalizesPositionAndIncludesDepartment() {
        UUID departmentId = UUID.randomUUID();
        Department department = activeDepartment(departmentId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(positionRepository.existsByCode("SALES_STAFF")).thenReturn(false);
        when(positionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Position position = invocation.getArgument(0);
            position.setId(UUID.randomUUID());
            return position;
        });

        PositionDto result = service.create(new CreatePositionRequest(
                departmentId, " sales_staff ", " Nhân viên bán hàng ", null, true,
                3, 12_000_000L, 18_000_000L));

        assertEquals("SALES_STAFF", result.getCode());
        assertEquals("Nhân viên bán hàng", result.getName());
        assertEquals("Kinh doanh", result.getDepartmentName());
        assertEquals(3, result.getHierarchyLevel());
        assertEquals(12_000_000L, result.getMinSalary());
        assertEquals(18_000_000L, result.getMaxSalary());
    }

    @Test
    void createRejectsInactiveDepartment() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(
                Department.builder().id(departmentId).active(false).build()));

        assertThrows(BusinessException.class, () -> service.create(new CreatePositionRequest(
                departmentId, "DEV", "Lập trình viên", null, true, 1, 0L, 0L)));
        verify(positionRepository, never()).saveAndFlush(any());
    }

    @Test
    void createNormalizesAndRejectsDuplicateGlobalCode() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));
        when(positionRepository.existsByCode("LEAD")).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(new CreatePositionRequest(
                        departmentId, " lead ", "Trưởng nhóm", null, true,
                        3, 12_000_000L, 18_000_000L)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(positionRepository, never()).saveAndFlush(any());
    }

    @Test
    void createTranslatesConcurrentDuplicateCodeToConflict() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));
        when(positionRepository.existsByCode("LEAD")).thenReturn(false);
        when(positionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(new CreatePositionRequest(
                        departmentId, "LEAD", "Trưởng nhóm", null, true,
                        3, 12_000_000L, 18_000_000L)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void updateNeverChangesCode() {
        UUID departmentId = UUID.randomUUID();
        Position existing = position(departmentId, "LEAD", 3, 12_000_000L, 18_000_000L);
        when(positionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));
        when(positionRepository.save(existing)).thenReturn(existing);

        service.update(existing.getId(), new UpdatePositionRequest(
                departmentId, "Tên mới", null, true, 4, 15_000_000L, 20_000_000L, false));

        assertEquals("LEAD", existing.getCode());
        assertEquals("Tên mới", existing.getName());
        assertEquals(4, existing.getHierarchyLevel());
        assertEquals(15_000_000L, existing.getMinSalary());
        assertEquals(20_000_000L, existing.getMaxSalary());
    }

    @Test
    void createRejectsSalaryRangeWhenMaximumIsBelowMinimum() {
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(new CreatePositionRequest(
                        departmentId, "LEAD", "Trưởng nhóm", null, true,
                        3, 18_000_000L, 12_000_000L)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(positionRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsSalaryRangeWhenMaximumIsBelowMinimum() {
        UUID departmentId = UUID.randomUUID();
        Position existing = position(departmentId, "LEAD", 3, 12_000_000L, 18_000_000L);
        when(positionRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.update(existing.getId(), new UpdatePositionRequest(
                        departmentId, "Tên mới", null, true, 4, 20_000_000L, 15_000_000L, false)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(positionRepository, never()).save(any());
    }

    @Test
    void getByDepartmentReturnsAllPositionsForDepartment() {
        UUID departmentId = UUID.randomUUID();
        Department department = activeDepartment(departmentId);
        Position manager = Position.builder().id(UUID.randomUUID()).departmentId(departmentId)
                .code("MANAGER").name("Quản lý").active(true).build();
        Position staff = Position.builder().id(UUID.randomUUID()).departmentId(departmentId)
                .code("STAFF").name("Nhân viên").active(false).build();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(positionRepository.findAllByDepartmentIdOrderByNameAsc(departmentId))
                .thenReturn(List.of(staff, manager));

        var result = service.getByDepartment(departmentId);

        assertEquals(2, result.size());
        assertEquals("STAFF", result.get(0).getCode());
        assertEquals("Kinh doanh", result.get(0).getDepartmentName());
    }

    @Test
    void deleteSoftDeletesPositionAndRestoreReactivatesIt() {
        UUID departmentId = UUID.randomUUID();
        Position position = position(departmentId, "LEAD", 3, 10L, 20L);
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));
        when(positionRepository.save(position)).thenReturn(position);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(activeDepartment(departmentId)));

        service.delete(position.getId());
        assertEquals(false, position.getActive());
        verify(positionRepository, never()).delete(any());

        PositionDto restored = service.restore(position.getId());
        assertEquals(true, restored.getActive());
    }

    @Test
    void cachesOnlyPositionDetailOperationsByPositionId() throws NoSuchMethodException {
        Cacheable getById = PositionService.class.getMethod("getById", UUID.class)
                .getAnnotation(Cacheable.class);
        CachePut update = PositionService.class.getMethod("update", UUID.class, UpdatePositionRequest.class)
                .getAnnotation(CachePut.class);
        CacheEvict delete = PositionService.class.getMethod("delete", UUID.class)
                .getAnnotation(CacheEvict.class);

        assertNotNull(getById);
        assertArrayEquals(new String[] {CacheNames.POSITION_DETAIL}, getById.cacheNames());
        assertEquals("#id", getById.key());
        assertNotNull(update);
        assertArrayEquals(new String[] {CacheNames.POSITION_DETAIL}, update.cacheNames());
        assertEquals("#id", update.key());
        assertNotNull(delete);
        assertArrayEquals(new String[] {CacheNames.POSITION_DETAIL}, delete.cacheNames());
        assertEquals("#id", delete.key());
        assertNull(PositionService.class.getMethod(
                "getList", String.class, UUID.class, Boolean.class, org.springframework.data.domain.Pageable.class)
                .getAnnotation(Cacheable.class));
        assertNull(PositionService.class.getMethod("getByDepartment", UUID.class).getAnnotation(Cacheable.class));
    }

    private Department activeDepartment(UUID id) {
        return Department.builder().id(id).code("SALES").name("Kinh doanh").active(true).build();
    }

    private Position position(UUID departmentId, String code, int hierarchyLevel, long minSalary, long maxSalary) {
        return Position.builder().id(UUID.randomUUID()).departmentId(departmentId).code(code).name("Tên cũ")
                .active(true).hierarchyLevel(hierarchyLevel).minSalary(minSalary).maxSalary(maxSalary).build();
    }
}
