package com.fashionsystem.fashion_system.dto.auth;

import com.fashionsystem.fashion_system.entity.PasswordResetOtp.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Yêu cầu gửi hoặc gửi lại OTP quên mật khẩu. */
public record PasswordResetOtpRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull AccountType accountType) {
}
