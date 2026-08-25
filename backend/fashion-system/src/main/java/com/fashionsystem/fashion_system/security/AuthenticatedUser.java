package com.fashionsystem.fashion_system.security;

import java.io.Serializable;
import java.util.UUID;

/** Principal tin cậy được tạo từ JWT đã xác minh và dữ liệu user phía server. */
public record AuthenticatedUser(UUID userId, String username) implements Serializable {
}
