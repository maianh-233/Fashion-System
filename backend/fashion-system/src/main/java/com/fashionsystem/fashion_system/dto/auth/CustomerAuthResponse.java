package com.fashionsystem.fashion_system.dto.auth;

/** Kết quả xác thực dành riêng cho khách hàng. */
public record CustomerAuthResponse(
        String token, String tokenType, long expiresInMs, CustomerInfoResponse customer) {
}
