package com.fashionsystem.fashion_system.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Email của nhân viên cần gán vào một quản lý. */
public record SubordinateAssignmentRequest(
        @NotBlank @Email @Size(max = 255) String email) {
}
