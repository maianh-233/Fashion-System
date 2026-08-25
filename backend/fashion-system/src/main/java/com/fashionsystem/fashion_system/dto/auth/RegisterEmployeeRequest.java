package com.fashionsystem.fashion_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.fashionsystem.fashion_system.entity.EmploymentType;
import com.fashionsystem.fashion_system.entity.Gender;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Dữ liệu tạo tài khoản và hồ sơ nhân viên nội bộ. */
public record RegisterEmployeeRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 30) String employeeCode,
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 20) String phone,
        LocalDate dateOfBirth,
        Gender gender,
        @Size(max = 150) String jobTitle,
        EmploymentType employmentType,
        LocalDate hireDate,
        @Size(max = 150) String workLocation,
        UUID managerId,
        @NotEmpty Set<@NotBlank @Size(max = 50) String> roleCodes,
        Set<UUID> departmentIds) {
}
