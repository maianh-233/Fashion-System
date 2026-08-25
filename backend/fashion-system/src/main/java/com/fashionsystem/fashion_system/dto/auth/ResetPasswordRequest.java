package com.fashionsystem.fashion_system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reset token đã xác minh và mật khẩu mới. */
public record ResetPasswordRequest(
        @NotBlank @Size(max = 100) String resetToken,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
