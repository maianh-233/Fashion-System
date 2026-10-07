package com.fashionsystem.fashion_system.security;

import java.io.Serializable;
import java.util.UUID;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;

/** Principal tin cậy được tạo từ JWT đã xác minh và dữ liệu user phía server. */
public record AuthenticatedUser(UUID userId, String username) implements AuthenticatedAccount, Serializable {

    @Override
    public RealtimeIdentity realtimeIdentity() {
        return RealtimeIdentity.user(userId);
    }
}
