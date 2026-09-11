package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeDataScopeService {
    private static final Set<String> GLOBAL_ROLE_CODES = Set.of("ADMIN", "SUPER_ADMIN");

    private final AuthorizationService authorizationService;
    private final RoleRepository roleRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final StoreRepository storeRepository;

    public EmployeeDataScope resolve(UUID actorId, String permissionCode) {
        authorizationService.getPermissionScope(actorId, permissionCode)
                .orElseThrow(() -> BusinessException.forbidden("Bạn không có quyền quản lý nhân viên"));

        if (roleRepository.findCodesByUserId(actorId).stream().anyMatch(GLOBAL_ROLE_CODES::contains)) {
            return EmployeeDataScope.all(permissionCode);
        }

        List<UUID> storeIds = storeStaffRepository.findAllByUserIdAndActiveTrue(actorId).stream()
                .map(assignment -> assignment.getStoreId())
                .distinct()
                .toList();
        if (storeIds.isEmpty()) {
            return EmployeeDataScope.all(permissionCode);
        }
        if (storeIds.size() > 1) {
            throw BusinessException.forbidden("Tài khoản được gán nhiều cửa hàng nên phạm vi quản lý không rõ ràng");
        }

        Store store = storeRepository.findById(storeIds.getFirst())
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> BusinessException.forbidden("Cửa hàng quản lý không còn hoạt động"));
        return EmployeeDataScope.store(store, permissionCode);
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
        if (!storeStaffRepository.existsByUserIdAndStoreIdAndActiveTrue(targetUserId, scope.storeId())) {
            throw BusinessException.forbidden("Bạn không có quyền quản lý nhân viên này");
        }
    }
}
