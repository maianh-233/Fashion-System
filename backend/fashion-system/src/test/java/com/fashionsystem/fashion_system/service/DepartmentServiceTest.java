package com.fashionsystem.fashion_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.DepartmentMapper;
import com.fashionsystem.fashion_system.repository.DepartmentRepository;
import com.fashionsystem.fashion_system.repository.UserDepartmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    @Mock DepartmentRepository departmentRepository;
    @Mock UserDepartmentRepository userDepartmentRepository;
    private DepartmentService service;

    @BeforeEach
    void setUp() {
        service = new DepartmentService(
                departmentRepository,
                new DepartmentMapper(),
                userDepartmentRepository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(departmentRepository.existsByCode("SALES")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.create(DepartmentDto.builder().code("sales ").name("Phòng Kinh Doanh").build()));

        assertEquals("Mã phòng ban đã tồn tại", exception.getReason());
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void updateKeepsOriginalDepartmentCode() {
        UUID id = UUID.randomUUID();
        Department department = Department.builder()
                .id(id).code("SALES").name("Kinh doanh").active(true).build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DepartmentDto result = service.update(id, DepartmentDto.builder()
                .code("HACKED")
                .name("Kinh doanh toàn quốc")
                .active(true)
                .build());

        assertEquals("SALES", result.getCode());
        assertEquals("Kinh doanh toàn quốc", result.getName());
    }

    @Test
    void updateRejectsDeactivationWhenDepartmentHasActiveEmployee() {
        UUID id = UUID.randomUUID();
        Department department = Department.builder()
                .id(id).code("SALES").name("Kinh doanh").active(true).build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));
        when(userDepartmentRepository.existsActiveEmployeeByDepartmentId(id)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.update(id, DepartmentDto.builder()
                .code("SALES").name("Kinh doanh").active(false).build()));

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void deleteRejectsDepartmentWithActiveEmployee() {
        UUID id = UUID.randomUUID();
        Department department = Department.builder()
                .id(id).code("SALES").name("Kinh doanh").active(true).build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));
        when(userDepartmentRepository.existsActiveEmployeeByDepartmentId(id)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.delete(id));

        verify(departmentRepository, never()).save(any());
        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void deleteSoftDeletesDepartmentWithoutActiveEmployee() {
        UUID id = UUID.randomUUID();
        Department department = Department.builder()
                .id(id).code("SALES").name("Kinh doanh").active(true).build();
        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));
        when(userDepartmentRepository.existsActiveEmployeeByDepartmentId(id)).thenReturn(false);
        when(departmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(id);

        assertFalse(department.getActive());
        verify(departmentRepository).save(department);
        verify(departmentRepository, never()).delete(any());
    }
}
