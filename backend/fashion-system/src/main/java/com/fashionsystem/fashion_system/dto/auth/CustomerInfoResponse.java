package com.fashionsystem.fashion_system.dto.auth;

import java.util.UUID;

/** Thông tin khách hàng trả về sau xác thực, hoàn toàn độc lập với User. */
public record CustomerInfoResponse(UUID id, String username, String email, String fullName) {
}
