package com.fashionsystem.fashion_system.dto.profile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Hồ sơ an toàn của tài khoản nội bộ đang đăng nhập. */
public record AdminProfileResponse(
        UUID id,
        String username,
        String employeeCode,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String gender,
        String avatar,
        String jobTitle,
        String employmentType,
        String employmentStatus,
        LocalDate hireDate,
        String workLocation,
        Boolean active,
        Boolean locked,
        Boolean emailVerified,
        Boolean phoneVerified,
        LocalDateTime lastPasswordChange,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        List<RoleSummary> roles) {

    public record RoleSummary(String code, String name) {
    }
}
