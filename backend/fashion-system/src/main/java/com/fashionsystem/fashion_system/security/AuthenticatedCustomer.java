package com.fashionsystem.fashion_system.security;

import java.io.Serializable;
import java.util.UUID;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;

/** Principal riêng của khách hàng; không thể bị nhầm với nhân viên. */
public record AuthenticatedCustomer(UUID customerId, String username) implements AuthenticatedAccount, Serializable {

    @Override
    public RealtimeIdentity realtimeIdentity() {
        return RealtimeIdentity.customer(customerId);
    }
}
