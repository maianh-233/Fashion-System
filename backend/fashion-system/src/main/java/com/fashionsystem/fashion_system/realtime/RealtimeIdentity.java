package com.fashionsystem.fashion_system.realtime;

import java.util.Objects;
import java.util.UUID;

/** Transport-neutral address of an authenticated realtime recipient. */
public record RealtimeIdentity(RealtimeAccountType accountType, UUID accountId) {

    public RealtimeIdentity {
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
    }

    public static RealtimeIdentity user(UUID userId) {
        return new RealtimeIdentity(RealtimeAccountType.USER, userId);
    }

    public static RealtimeIdentity customer(UUID customerId) {
        return new RealtimeIdentity(RealtimeAccountType.CUSTOMER, customerId);
    }

    public String canonicalPrincipal() {
        return accountType.name() + ":" + accountId;
    }
}
