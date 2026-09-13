package com.fashionsystem.fashion_system.dto.employee;

import com.fashionsystem.fashion_system.entity.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Các trường quản trị được phép cập nhật trên tài khoản nhân viên. */
public record UpdateEmployeeRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @Size(min = 8, max = 72) String newPassword,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(
                regexp = "^(?:\\+84|0)(?:3|5|7|8|9)\\d{8}$",
                message = "Số điện thoại không đúng định dạng Việt Nam")
        @Size(max = 20) String phone,
        @Size(max = 150) String jobTitle,
        @NotNull EmploymentType employmentType,
        UUID departmentId,
        UUID positionId,
        LocalDate hireDate,
        @Pattern(regexp = "ACTIVE|PROBATION|ON_LEAVE|SUSPENDED|TERMINATED") String employmentStatus,
        @NotNull Boolean active,
        @NotNull Boolean locked,
        @NotEmpty Set<@NotBlank @Size(max = 50) String> roleCodes,
        UUID storeId,
        Boolean resetInvalidRelations) {
}
