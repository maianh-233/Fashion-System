package com.fashionsystem.fashion_system.security;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.io.Serializable;
import java.security.Principal;

/** Principal contract shared by HTTP and realtime transports. */
public interface AuthenticatedAccount extends Principal, Serializable {

    RealtimeIdentity realtimeIdentity();

    @Override
    default String getName() {
        return realtimeIdentity().canonicalPrincipal();
    }
}
