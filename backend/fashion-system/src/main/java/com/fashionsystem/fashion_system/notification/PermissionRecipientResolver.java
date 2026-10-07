package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reusable recipient lookup by effective permission, Store identity scope, and account state. */
@Service
@RequiredArgsConstructor
public class PermissionRecipientResolver {

    private final PermissionRecipientRepository repository;

    @Transactional(readOnly = true)
    public Set<RealtimeIdentity> resolveUsers(
            String permissionCode, UUID storeId, UUID excludedUserId) {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("permissionCode must not be blank");
        }
        Objects.requireNonNull(storeId, "storeId must not be null");
        String normalizedPermission = permissionCode.trim().toUpperCase(Locale.ROOT);
        return repository.findEligibleUserIds(normalizedPermission, storeId, excludedUserId).stream()
                .filter(Objects::nonNull)
                .filter(userId -> !userId.equals(excludedUserId))
                .map(RealtimeIdentity::user)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
