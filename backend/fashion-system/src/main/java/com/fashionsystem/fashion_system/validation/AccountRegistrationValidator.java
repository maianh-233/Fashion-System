package com.fashionsystem.fashion_system.validation;

import com.fashionsystem.fashion_system.dto.auth.RegisterEmployeeRequest;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Business validation và batch lookup dùng chung cho các luồng đăng ký tài khoản. */
@Component
@RequiredArgsConstructor
public class AccountRegistrationValidator {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeRelations validateEmployee(
            RegisterEmployeeRequest request,
            String username,
            String email,
            String employeeCode,
            String phone,
            Set<String> roleCodes) {
        validateEmployeeDuplicates(username, email, employeeCode, phone);
        validateManager(request.managerId());
        return new EmployeeRelations(loadRoles(roleCodes), loadDepartmentIds(request.departmentIds()));
    }

    public void validateUser(String username, String email) {
        List<User> conflicts = userRepository.findRegistrationConflicts(username, email, null, null);
        if (conflicts.stream().anyMatch(existing -> username.equals(existing.getUsername()))) {
            throw BusinessException.conflict("Username đã tồn tại");
        }
        if (conflicts.stream().anyMatch(existing -> email.equals(existing.getEmail()))) {
            throw BusinessException.conflict("Email đã tồn tại");
        }
    }

    public void validateCustomer(String username, String email) {
        List<Customer> conflicts = customerRepository.findRegistrationConflicts(username, email);
        if (conflicts.stream().anyMatch(existing -> username.equals(existing.getUsername()))) {
            throw BusinessException.conflict("Username khách hàng đã tồn tại");
        }
        if (conflicts.stream().anyMatch(existing -> email.equals(existing.getEmail()))) {
            throw BusinessException.conflict("Email khách hàng đã tồn tại");
        }
    }

    private void validateEmployeeDuplicates(String username, String email, String employeeCode, String phone) {
        List<User> conflicts = userRepository.findRegistrationConflicts(username, email, employeeCode, phone);
        if (conflicts.stream().anyMatch(existing -> username.equals(existing.getUsername()))) {
            throw BusinessException.conflict("Username nhân viên đã tồn tại");
        }
        if (conflicts.stream().anyMatch(existing -> email.equals(existing.getEmail()))) {
            throw BusinessException.conflict("Email nhân viên đã tồn tại");
        }
        if (conflicts.stream().anyMatch(existing -> employeeCode.equals(existing.getEmployeeCode()))) {
            throw BusinessException.conflict("Mã nhân viên đã tồn tại");
        }
        if (phone != null && conflicts.stream().anyMatch(existing -> phone.equals(existing.getPhone()))) {
            throw BusinessException.conflict("Số điện thoại nhân viên đã tồn tại");
        }
    }

    private void validateManager(UUID managerId) {
        if (managerId != null && !userRepository.existsById(managerId)) {
            throw BusinessException.badRequest("Quản lý trực tiếp không tồn tại");
        }
    }

    private List<Role> loadRoles(Set<String> requestedCodes) {
        List<Role> roles = roleRepository.findAllByCodeIn(requestedCodes);
        Set<String> foundCodes = roles.stream().map(Role::getCode).collect(Collectors.toSet());
        requestedCodes.stream().filter(code -> !foundCodes.contains(code)).sorted().findFirst()
                .ifPresent(code -> { throw BusinessException.badRequest("Role không tồn tại: " + code); });
        Map<String, Role> byCode = roles.stream()
                .collect(Collectors.toMap(Role::getCode, Function.identity()));
        return requestedCodes.stream().sorted().map(byCode::get).toList();
    }

    private Set<UUID> loadDepartmentIds(Set<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) return Set.of();
        Set<UUID> departmentIds = new HashSet<>();
        departmentRepository.findAllById(requestedIds).forEach(department -> departmentIds.add(department.getId()));
        requestedIds.stream().filter(id -> !departmentIds.contains(id)).sorted().findFirst()
                .ifPresent(id -> { throw BusinessException.badRequest("Phòng ban không tồn tại: " + id); });
        return Set.copyOf(departmentIds);
    }

    public record EmployeeRelations(List<Role> roles, Set<UUID> departmentIds) {
    }
}
