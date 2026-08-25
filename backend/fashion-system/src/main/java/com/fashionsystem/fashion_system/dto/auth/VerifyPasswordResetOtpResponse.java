package com.fashionsystem.fashion_system.dto.auth;

/** Reset token chỉ được cấp sau khi OTP hợp lệ. */
public record VerifyPasswordResetOtpResponse(String resetToken, long expiresInSeconds) {
}
