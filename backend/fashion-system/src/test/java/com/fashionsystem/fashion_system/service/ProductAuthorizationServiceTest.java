package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Catches Product mutation authorization that checks RBAC but forgets Global identity scope. */
@ExtendWith(MockitoExtension.class)
class ProductAuthorizationServiceTest {
    @Mock AuthorizationService authorizationService;
    @Mock UserScopeService userScopeService;

    private ProductAuthorizationService service;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new ProductAuthorizationService(authorizationService, userScopeService);
        actorId = UUID.randomUUID();
    }

    @Test
    void storeUserWithDatabaseMutationPermissionIsStillDenied() {
        when(authorizationService.hasPermission(actorId, "PRODUCT_UPDATE")).thenReturn(true);
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);

        assertThatThrownBy(() -> service.requireMutation(actorId, "PRODUCT_UPDATE"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void missingMutationPermissionFailsBeforeScopeLookup() {
        when(authorizationService.hasPermission(actorId, "PRODUCT_DELETE")).thenReturn(false);

        assertThatThrownBy(() -> service.requireMutation(actorId, "PRODUCT_DELETE"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        verify(userScopeService, never()).requireGlobal(actorId);
    }

    @Test
    void readNeedsPermissionButDoesNotRequireGlobalIdentity() {
        when(authorizationService.hasPermission(actorId, "PRODUCT_VIEW")).thenReturn(true);

        service.requireRead(actorId, "PRODUCT_VIEW");

        verify(userScopeService, never()).requireGlobal(actorId);
    }

    @Test
    void globalMutationWithPermissionIsAllowed() {
        when(authorizationService.hasPermission(actorId, "PRODUCT_CREATE")).thenReturn(true);

        service.requireMutation(actorId, "PRODUCT_CREATE");

        verify(userScopeService).requireGlobal(actorId);
    }
}
