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

    public List<StoreOption> accessibleStores(UUID actor) {
        UserScope scope = scopes.resolve(actor);
        var permissions = List.of("INVENTORY_VIEW", "IMPORT_RECEIPT_VIEW", "EXPORT_RECEIPT_VIEW");
        if (permissions.stream().noneMatch(p -> permits(actor, scope, p)))
            throw BusinessException.forbidden("Bạn không có quyền xem kho");
        return stores.findAllByActiveTrueOrderByNameAsc().stream()
            .filter(s -> scope.isGlobal() || s.getId().equals(scope.storeId()))
            .map(s -> new StoreOption(s.getId(), s.getName())).toList();
    }

    private boolean permits(UUID actor, UserScope identity, String action) {
        PermissionScope required = identity.isGlobal() ? PermissionScope.ALL : PermissionScope.STORE;
        return authorization.getPermissionScope(actor, action).filter(p -> p.covers(required)).isPresent();
    }

    public record StoreOption(UUID id, String name) {}
}
