package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Intersection of trusted employee assignment and the effective scope of each RBAC action. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreAccessService {
    private final UserScopeService scopes;
    private final AuthorizationService authorization;
    private final StoreRepository stores;

    public UUID require(UUID actor, UUID requestedStore, String permission) {
        return requireAny(actor, requestedStore, List.of(permission));
    }

    public UUID requireAny(UUID actor, UUID requestedStore, List<String> permissions) {
        UserScope scope = scopes.resolve(actor);
        UUID store = requestedStore == null && !scope.isGlobal() ? scope.storeId() : requestedStore;
        if (store == null) throw BusinessException.badRequest("Phải chọn một cửa hàng");
        if (!scope.isGlobal() && !store.equals(scope.storeId()))
            throw BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này");
        if (permissions.stream().noneMatch(p -> permits(actor, scope, p)))
            throw BusinessException.forbidden("Bạn không có quyền thực hiện thao tác tại cửa hàng này");
        stores.findById(store).filter(s -> Boolean.TRUE.equals(s.getActive()))
            .orElseThrow(() -> BusinessException.notFound("Cửa hàng không hoạt động hoặc không tồn tại"));
        return store;
    }

    /** Như {@link #require} nhưng nhân viên Global được bỏ trống cửa hàng để xem toàn hệ thống (trả về null). */
    public UUID requireOrAll(UUID actor, UUID requestedStore, String permission) {
        UserScope scope = scopes.resolve(actor);
        if (requestedStore != null || !scope.isGlobal()) return require(actor, requestedStore, permission);
        if (!permits(actor, scope, permission))
            throw BusinessException.forbidden("Bạn không có quyền xem dữ liệu toàn hệ thống");
        return null;
    }

    /** Quyền trên phạm vi đã chọn bằng {@link #requireOrAll}: null là toàn hệ thống (chỉ Global). */
    public boolean allowsScope(UUID actor, UUID storeOrAll, String permission) {
        if (storeOrAll != null) return allows(actor, storeOrAll, permission);
        UserScope scope = scopes.resolve(actor);
        return scope.isGlobal() && permits(actor, scope, permission);
    }

    public List<StoreOption> accessibleStores(UUID actor) {
        return accessibleStores(actor, List.of("INVENTORY_VIEW", "IMPORT_RECEIPT_VIEW", "EXPORT_RECEIPT_VIEW"),
                "Bạn không có quyền xem kho");
    }

    /** Cửa hàng nhân viên được chọn cho một module: Global thấy mọi cửa hàng, Store chỉ thấy cửa hàng mình. */
    public List<StoreOption> accessibleStores(UUID actor, List<String> permissions, String deniedMessage) {
        UserScope scope = scopes.resolve(actor);
        if (permissions.stream().noneMatch(p -> permits(actor, scope, p)))
            throw BusinessException.forbidden(deniedMessage);
        return stores.findAllByActiveTrueOrderByNameAsc().stream()
            .filter(s -> scope.isGlobal() || s.getId().equals(scope.storeId()))
            .map(s -> new StoreOption(s.getId(), s.getName())).toList();
    }

    /** Như {@link #require} nhưng trả về false thay vì ném lỗi; không kiểm tra cửa hàng còn hoạt động. */
    public boolean allows(UUID actor, UUID store, String permission) {
        UserScope scope = scopes.resolve(actor);
        if (store == null || (!scope.isGlobal() && !store.equals(scope.storeId()))) return false;
        return permits(actor, scope, permission);
    }

    private boolean permits(UUID actor, UserScope identity, String action) {
        PermissionScope required = identity.isGlobal() ? PermissionScope.ALL : PermissionScope.STORE;
        return authorization.getPermissionScope(actor, action).filter(p -> p.covers(required)).isPresent();
    }

    public record StoreOption(UUID id, String name) {}
}
