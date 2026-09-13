package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeDataScopeService {
    private final AuthorizationService authorizationService;
    private final UserScopeService userScopeService;

    public EmployeeDataScope resolve(UUID actorId, String permissionCode) {
        PermissionScope permissionScope = authorizationService.getPermissionScope(actorId, permissionCode)
                .orElseThrow(() -> BusinessException.forbidden("Bạn không có quyền quản lý nhân viên"));

        UserScope identityScope = userScopeService.resolve(actorId);
        if (!identityScope.isGlobal()) {
            return EmployeeDataScope.store(identityScope, permissionCode);
        }
        if (permissionScope == PermissionScope.ALL) {
            return EmployeeDataScope.all(permissionCode);
        }
        throw BusinessException.forbidden(
                "Tài khoản có phạm vi cửa hàng nhưng chưa được gán cửa hàng hoạt động");
    }

    public UUID validateFilter(EmployeeDataScope scope, UUID requestedStoreId) {
        if (scope.isGlobal()) {
            return requestedStoreId;
        }
        if (requestedStoreId != null && !requestedStoreId.equals(scope.storeId())) {
            throw BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này");
        }
        return scope.storeId();
    }

    public void requireTarget(EmployeeDataScope scope, UUID targetUserId) {
        if (scope.isGlobal()) {
            return;
        }
        userScopeService.requireUserInStore(targetUserId, scope.storeId());
    }
}
