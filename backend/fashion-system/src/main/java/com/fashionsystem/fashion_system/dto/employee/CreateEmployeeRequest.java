package com.fashionsystem.fashion_system.dto.employee;

import com.fashionsystem.fashion_system.entity.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Dữ liệu tạo nhân viên và phân công cửa hàng trong cùng một giao dịch. */
public record CreateEmployeeRequest(
        @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Pattern(
                regexp = "^(?:\\+84|0)(?:3|5|7|8|9)\\d{8}$",
                message = "Số điện thoại không đúng định dạng Việt Nam")
        @Size(max = 20) String phone,
        @Size(max = 150) String jobTitle,
        @NotNull EmploymentType employmentType,
        UUID departmentId,
        UUID positionId,
        @NotEmpty Set<@NotBlank @Size(max = 50) String> roleCodes,
        UUID storeId) {
}
