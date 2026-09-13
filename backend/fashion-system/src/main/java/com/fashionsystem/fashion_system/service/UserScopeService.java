package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves trusted Global/Store identity scope from active Store assignments. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserScopeService {
    private final StoreStaffRepository storeStaffRepository;
    private final StoreRepository storeRepository;

    /**
     * Resolves identity scope without using role names or an RBAC permission's configured scope.
     *
     * @param userId authenticated internal user id
     * @return Global scope or one active Store scope
     */
    public UserScope resolve(UUID userId) {
        if (userId == null) {
            throw BusinessException.forbidden("Không xác định được nhân viên hiện tại");
        }
        List<UUID> storeIds = storeStaffRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(assignment -> assignment.getStoreId())
                .distinct()
                .toList();
        if (storeIds.isEmpty()) {
            return UserScope.global();
        }
        if (storeIds.size() > 1) {
            throw BusinessException.forbidden(
                    "Tài khoản được gán nhiều cửa hàng nên phạm vi dữ liệu không rõ ràng");
        }
        Store store = storeRepository.findById(storeIds.getFirst())
                .filter(value -> Boolean.TRUE.equals(value.getActive()))
                .orElseThrow(() -> BusinessException.forbidden("Cửa hàng của nhân viên không còn hoạt động"));
        return UserScope.store(store);
    }

    /** Requires the authenticated employee to have chain-wide identity scope. */
    public void requireGlobal(UUID userId) {
        if (!resolve(userId).isGlobal()) {
            throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm");
        }
    }

    /**
     * Resolves an optional Store filter. Store employees are forced to their own Store and attempts
     * to submit another Store are rejected.
     */
    public UUID resolveStoreId(UUID userId, UUID requestedStoreId) {
        UserScope scope = resolve(userId);
        if (scope.isGlobal()) {
            return requestedStoreId;
        }
        if (requestedStoreId != null && !requestedStoreId.equals(scope.storeId())) {
            throw BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này");
        }
        return scope.storeId();
    }

    /** Requires access to the Store id loaded from a persisted business resource. */
    public void requireStoreAccess(UUID userId, UUID persistedStoreId) {
        UserScope scope = resolve(userId);
        if (!scope.isGlobal() && !scope.storeId().equals(persistedStoreId)) {
            throw BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này");
        }
    }

    /** Requires a target employee to have an active assignment in the supplied Store. */
    public void requireUserInStore(UUID targetUserId, UUID storeId) {
        if (!storeStaffRepository.existsByUserIdAndStoreIdAndActiveTrue(targetUserId, storeId)) {
            throw BusinessException.forbidden("Bạn không có quyền quản lý nhân viên này");
        }
    }
}
