package com.fashionsystem.fashion_system.dto.auth;

import java.util.Set;
import java.util.UUID;

/** Kết quả tạo nhân viên; không phát JWT để tránh admin mạo danh nhân viên mới. */
public record EmployeeRegistrationResponse(
        UUID id,
        String username,
        String employeeCode,
        String fullName,
        String email,
        Set<String> roles,
        Set<UUID> departmentIds) {
}
