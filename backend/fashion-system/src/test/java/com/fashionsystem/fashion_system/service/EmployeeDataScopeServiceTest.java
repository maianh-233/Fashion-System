package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Ensures employee administration reuses identity scope instead of treating RBAC ALL as identity. */
@ExtendWith(MockitoExtension.class)
class EmployeeDataScopeServiceTest {
    private static final String PERMISSION = "USER_VIEW";

    @Mock AuthorizationService authorizationService;
    @Mock UserScopeService userScopeService;

    private EmployeeDataScopeService service;
    private UUID actorId;
    private UUID storeId;
    private UUID otherStoreId;

    @BeforeEach
    void setUp() {
        service = new EmployeeDataScopeService(authorizationService, userScopeService);
        actorId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        otherStoreId = UUID.randomUUID();
    }

    @Test
    void globalIdentityWithAllPermissionResolvesGlobal() {
        allow(PermissionScope.ALL);
        when(userScopeService.resolve(actorId)).thenReturn(UserScope.global());

        EmployeeDataScope result = service.resolve(actorId, PERMISSION);

        assertThat(result.kind()).isEqualTo(EmployeeDataScope.Kind.ALL);
    }

    @Test
    void storeIdentityStaysStoreScopedEvenWhenPermissionScopeIsAll() {
        allow(PermissionScope.ALL);
        when(userScopeService.resolve(actorId)).thenReturn(storeScope());

        EmployeeDataScope result = service.resolve(actorId, PERMISSION);

        assertThat(result.kind()).isEqualTo(EmployeeDataScope.Kind.STORE);
        assertThat(result.storeId()).isEqualTo(storeId);
    }

    @Test
    void globalIdentityCannotUseStorePermissionWithoutStoreAssignment() {
        allow(PermissionScope.STORE);
        when(userScopeService.resolve(actorId)).thenReturn(UserScope.global());

        assertThatThrownBy(() -> service.resolve(actorId, PERMISSION))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("chưa được gán cửa hàng");
    }

    @Test
    void missingPermissionIsForbiddenBeforeScopeResolution() {
        when(authorizationService.getPermissionScope(actorId, PERMISSION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(actorId, PERMISSION))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        verify(userScopeService, never()).resolve(actorId);
    }

    @Test
    void storeFilterRejectsAnotherStore() {
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(), PERMISSION);

        assertThatThrownBy(() -> service.validateFilter(scope, otherStoreId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void targetCheckDelegatesToSharedStoreAccessRule() {
        EmployeeDataScope scope = EmployeeDataScope.store(activeStore(), PERMISSION);
        UUID targetUserId = UUID.randomUUID();

        service.requireTarget(scope, targetUserId);

        verify(userScopeService).requireUserInStore(targetUserId, storeId);
    }

    private void allow(PermissionScope permissionScope) {
        when(authorizationService.getPermissionScope(actorId, PERMISSION))
                .thenReturn(Optional.of(permissionScope));
    }

    private UserScope storeScope() {
        return UserScope.store(activeStore());
    }

    private Store activeStore() {
        return Store.builder().id(storeId).code("SGC").name("Saigon Centre").active(true).build();
    }
}
