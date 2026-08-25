package com.fashionsystem.fashion_system.dto.auth;

import java.util.UUID;
import java.util.List;

/** Thông tin người dùng an toàn được trả về, không chứa mật khẩu. */
public record UserInfoResponse(UUID id, String username, String email, List<String> roles) {
}
