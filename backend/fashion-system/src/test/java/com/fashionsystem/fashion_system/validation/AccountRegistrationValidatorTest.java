package com.fashionsystem.fashion_system.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.dto.auth.RegisterEmployeeRequest;
import com.fashionsystem.fashion_system.entity.Department;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountRegistrationValidatorTest {
    @Mock UserRepository userRepository;
    @Mock CustomerRepository customerRepository;
    @Mock RoleRepository roleRepository;
    @Mock DepartmentRepository departmentRepository;

    private AccountRegistrationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AccountRegistrationValidator(
                userRepository, customerRepository, roleRepository, departmentRepository);
    }

    @Test
    void preservesDuplicateValidationPriorityRegardlessOfQueryOrder() {
        User emailConflict = User.builder().username("other").email("a@company.test").build();
        User usernameConflict = User.builder().username("alice").email("other@company.test").build();
        when(userRepository.findRegistrationConflicts("alice", "a@company.test", null, null))
                .thenReturn(List.of(emailConflict, usernameConflict));

        assertThatThrownBy(() -> validator.validateUser("alice", "a@company.test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username đã tồn tại");
    }

    @Test
    void loadsRolesAndDepartmentsInBatches() {
        UUID departmentId = UUID.randomUUID();
        RegisterEmployeeRequest request = new RegisterEmployeeRequest(
                "alice", "a@company.test", "password", "EMP-1", "Alice", null,
                null, null, null, null, null, null, null,
                Set.of("STAFF"), Set.of(departmentId));
        Role role = Role.builder().id(UUID.randomUUID()).code("STAFF").build();
        Department department = Department.builder().id(departmentId).build();
        when(userRepository.findRegistrationConflicts(
                "alice", "a@company.test", "EMP-1", null)).thenReturn(List.of());
        when(roleRepository.findAllByCodeIn(Set.of("STAFF"))).thenReturn(List.of(role));
        when(departmentRepository.findAllById(Set.of(departmentId))).thenReturn(List.of(department));

        var relations = validator.validateEmployee(
                request, "alice", "a@company.test", "EMP-1", null, Set.of("STAFF"));

        assertThat(relations.roles()).containsExactly(role);
        assertThat(relations.departmentIds()).containsExactly(departmentId);
        verify(roleRepository, times(1)).findAllByCodeIn(Set.of("STAFF"));
        verify(departmentRepository, times(1)).findAllById(Set.of(departmentId));
    }
}
