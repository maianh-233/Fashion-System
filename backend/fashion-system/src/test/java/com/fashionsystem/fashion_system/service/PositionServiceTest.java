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

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.PositionDto;
import com.fashionsystem.fashion_system.dto.position.CreatePositionRequest;
import com.fashionsystem.fashion_system.dto.position.UpdatePositionRequest;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Position;
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
    private PositionService service;

    @BeforeEach
    void setUp() {
        service = new PositionService(positionRepository, departmentRepository, new PositionMapper());
    }

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
