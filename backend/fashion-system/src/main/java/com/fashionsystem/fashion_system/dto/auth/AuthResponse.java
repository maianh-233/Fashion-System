package com.fashionsystem.fashion_system.dto.auth;

/** Kết quả xác thực gồm JWT và thông tin người dùng cơ bản. */
public record AuthResponse(String token, String tokenType, long expiresInMs, UserInfoResponse user) {
}
