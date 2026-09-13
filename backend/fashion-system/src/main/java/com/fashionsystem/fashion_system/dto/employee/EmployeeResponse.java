package com.fashionsystem.fashion_system.dto.employee;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Hồ sơ nhân viên dùng riêng cho màn hình quản trị nhân sự. */
public record EmployeeResponse(
        UUID id,
        String username,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        String jobTitle,
        String employmentType,
        String employmentStatus,
        LocalDate hireDate,
        Boolean active,
        Boolean locked,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        List<String> roleCodes,
        UUID storeId,
        String storeCode,
        String storeName,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        UUID positionId,
        String positionCode,
        String positionName,
        UUID managerId,
        String managerName) {
}
