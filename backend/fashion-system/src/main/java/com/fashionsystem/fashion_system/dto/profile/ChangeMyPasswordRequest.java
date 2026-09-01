package com.fashionsystem.fashion_system.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dữ liệu đổi mật khẩu của tài khoản đang đăng nhập. */
public record ChangeMyPasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
}
