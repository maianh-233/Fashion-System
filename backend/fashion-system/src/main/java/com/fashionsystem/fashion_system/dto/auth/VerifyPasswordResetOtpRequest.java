package com.fashionsystem.fashion_system.dto.auth;

import com.fashionsystem.fashion_system.entity.PasswordResetOtp.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** OTP do người dùng nhập để đổi lấy reset token ngắn hạn. */
public record VerifyPasswordResetOtpRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull AccountType accountType,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP phai gom 6 chu so") String otp) {
}
