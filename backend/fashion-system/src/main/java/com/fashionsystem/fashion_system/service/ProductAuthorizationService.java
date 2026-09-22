package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Combines effective RBAC permissions with Product master identity-scope rules. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductAuthorizationService {
    private final AuthorizationService authorizationService;
    private final UserScopeService userScopeService;

    /** Requires an effective read permission; both Global and Store employees may pass. */
    public void requireRead(UUID actorId, String permissionCode) {
        requirePermission(actorId, permissionCode);
    }

    public String visibleStatus(UUID actorId, String requestedStatus) {
        if (!userScopeService.resolve(actorId).isGlobal()) return "ACTIVE";
        if (requestedStatus == null || requestedStatus.isBlank()) return "ACTIVE";
        return "ALL".equalsIgnoreCase(requestedStatus) ? "" : requestedStatus;
    }

    public void requireActiveForStore(UUID actorId, boolean active) {
        if (!active && !userScopeService.resolve(actorId).isGlobal()) {
            throw BusinessException.forbidden("Dữ liệu catalog đã vô hiệu hóa");
        }
    }

    public Boolean visibleActive(UUID actorId, Boolean requestedActive) {
        if (!userScopeService.resolve(actorId).isGlobal()) return true;
        return requestedActive == null ? true : requestedActive;
    }

    /** Requires both the effective mutation permission and Global employee identity. */
    public void requireMutation(UUID actorId, String permissionCode) {
        requirePermission(actorId, permissionCode);
        userScopeService.requireGlobal(actorId);
    }

    private void requirePermission(UUID actorId, String permissionCode) {
        if (!authorizationService.hasPermission(actorId, permissionCode)) {
            throw BusinessException.forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }
}
