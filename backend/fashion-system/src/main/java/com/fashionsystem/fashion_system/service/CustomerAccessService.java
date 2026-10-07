package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Kết hợp RBAC hiện tại với Global/Store identity scope cho Customer. */
@Service
@RequiredArgsConstructor
public class CustomerAccessService {
    private final UserScopeService scopes;
    private final AuthorizationService authorization;

    public record QueryScope(UUID storeId, boolean noStore) {}

    public QueryScope resolveList(UUID actor, UUID requestedStore, boolean noStore) {
        UserScope identity = requirePermission(actor, "CUSTOMER_VIEW");
        if (identity.isGlobal()) return new QueryScope(requestedStore, noStore);
        if (noStore || requestedStore != null && !requestedStore.equals(identity.storeId())) {
            throw BusinessException.forbidden("Bạn không có quyền xem khách hàng ngoài cửa hàng của mình");
        }
        return new QueryScope(identity.storeId(), false);
    }

    public UUID resolveCreateStore(UUID actor, UUID requestedStore) {
        UserScope identity = requirePermission(actor, "CUSTOMER_CREATE");
        if (!identity.isGlobal()) {
            if (requestedStore != null && !requestedStore.equals(identity.storeId())) {
                throw BusinessException.forbidden("Bạn không có quyền tạo khách hàng cho cửa hàng khác");
            }
            return identity.storeId();
        }
        if (requestedStore == null) throw BusinessException.badRequest("Phải chọn cửa hàng nguồn");
        return requestedStore;
    }

    public void requireView(UUID actor, Customer customer) {
        UserScope identity = requirePermission(actor, "CUSTOMER_VIEW");
        requireOwned(identity, customer, "xem");
    }

    public void requireEdit(UUID actor, Customer customer) {
        UserScope identity = requirePermission(actor, "CUSTOMER_UPDATE");
        requireOwned(identity, customer, "sửa");
    }

    public void requireStatus(UUID actor, Customer customer) {
        UserScope identity = requirePermission(actor, "CUSTOMER_STATUS_MANAGE");
        requireOwned(identity, customer, "đổi trạng thái");
    }

    public void requireTier(UUID actor, Customer customer) {
        UserScope identity = requirePermission(actor, "CUSTOMER_TIER_MANAGE");
        if (!identity.isGlobal()) {
            throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi hạng khách hàng");
        }
    }

    public UserScope requireLookup(UUID actor) {
        return requirePermission(actor, "CUSTOMER_VIEW");
    }

    private UserScope requirePermission(UUID actor, String code) {
        UserScope identity = scopes.resolve(actor);
        PermissionScope required = identity.isGlobal() ? PermissionScope.ALL : PermissionScope.STORE;
        if (!authorization.hasPermission(actor, code, required)) {
            throw BusinessException.forbidden("Bạn không có quyền thao tác khách hàng");
        }
        return identity;
    }

    private void requireOwned(UserScope identity, Customer customer, String action) {
        if (identity.isGlobal()) return;
        if (customer.getSource() != CustomerSource.STORE
                || customer.getOriginStoreId() == null
                || !customer.getOriginStoreId().equals(identity.storeId())) {
            throw BusinessException.forbidden("Bạn không có quyền " + action + " khách hàng này");
        }
    }
}

