package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.auth.*;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.User;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Mapping response an toàn cho authentication; không ánh xạ password hash. */
@Component
public class AuthResponseMapper {
    public UserInfoResponse toUserInfo(User user, List<String> roleCodes) {
        return new UserInfoResponse(user.getId(), user.getUsername(), user.getEmail(),
                roleCodes.stream().map(role -> "ROLE_" + role).toList());
    }

    public CustomerInfoResponse toCustomerInfo(Customer customer) {
        return new CustomerInfoResponse(
                customer.getId(), customer.getUsername(), customer.getEmail(), customer.getFullName());
    }

    public EmployeeRegistrationResponse toEmployeeRegistration(
            User user, Set<String> roleCodes, Set<UUID> departmentIds) {
        return new EmployeeRegistrationResponse(
                user.getId(), user.getUsername(), user.getEmployeeCode(), user.getFullName(),
                user.getEmail(), roleCodes, departmentIds);
    }
}
