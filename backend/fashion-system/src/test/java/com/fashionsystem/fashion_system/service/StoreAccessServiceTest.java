package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class StoreAccessServiceTest {
    final UserScopeService scopes = mock(UserScopeService.class);
    final AuthorizationService permissions = mock(AuthorizationService.class);
    final StoreRepository stores = mock(StoreRepository.class);
    final UUID actor = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
    final StoreAccessService service = new StoreAccessService(scopes, permissions, stores);

    @Test void storeEmployeeCannotAccessAnotherStoreEvenWithAllGrant() {
        when(scopes.resolve(actor)).thenReturn(new UserScope(UserScope.Kind.STORE,a,"A","A"));
        assertThatThrownBy(() -> service.require(actor,b,"INVENTORY_VIEW"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }
    @Test void globalIdentityDoesNotTurnStoreGrantIntoAllAccess() {
        when(scopes.resolve(actor)).thenReturn(UserScope.global());
        when(permissions.getPermissionScope(actor,"INVENTORY_VIEW")).thenReturn(Optional.of(PermissionScope.STORE));
        assertThatThrownBy(() -> service.require(actor,a,"INVENTORY_VIEW")).isInstanceOf(BusinessException.class);
    }
    @Test void globalAllRequiresExplicitStore() {
        when(scopes.resolve(actor)).thenReturn(UserScope.global());
        assertThatThrownBy(() -> service.require(actor,null,"INVENTORY_VIEW")).isInstanceOf(BusinessException.class);
    }
    @Test void globalAllGrantMayViewWholeChainWithoutStore() {
        when(scopes.resolve(actor)).thenReturn(UserScope.global());
        when(permissions.getPermissionScope(actor,"DASHBOARD_VIEW")).thenReturn(Optional.of(PermissionScope.ALL));
        assertThat(service.requireOrAll(actor,null,"DASHBOARD_VIEW")).isNull();
        assertThat(service.allowsScope(actor,null,"DASHBOARD_VIEW")).isTrue();
        assertThat(service.allowsScope(actor,null,"INVENTORY_VIEW")).isFalse();
    }
    @Test void globalStoreGrantCannotViewWholeChain() {
        when(scopes.resolve(actor)).thenReturn(UserScope.global());
        when(permissions.getPermissionScope(actor,"DASHBOARD_VIEW")).thenReturn(Optional.of(PermissionScope.STORE));
        assertThatThrownBy(() -> service.requireOrAll(actor,null,"DASHBOARD_VIEW"))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }
    @Test void storeEmployeeWithoutStoreFallsBackToOwnStore() {
        when(scopes.resolve(actor)).thenReturn(new UserScope(UserScope.Kind.STORE,a,"A","A"));
        when(permissions.getPermissionScope(actor,"DASHBOARD_VIEW")).thenReturn(Optional.of(PermissionScope.STORE));
        when(stores.findById(a)).thenReturn(Optional.of(Store.builder().id(a).active(true).build()));
        assertThat(service.requireOrAll(actor,null,"DASHBOARD_VIEW")).isEqualTo(a);
        assertThat(service.allowsScope(actor,null,"DASHBOARD_VIEW")).isFalse();
        assertThatThrownBy(() -> service.requireOrAll(actor,b,"DASHBOARD_VIEW")).isInstanceOf(BusinessException.class);
    }
    @Test void storeGrantAllowsOwnActiveStore() {
        when(scopes.resolve(actor)).thenReturn(new UserScope(UserScope.Kind.STORE,a,"A","A"));
        when(permissions.getPermissionScope(actor,"INVENTORY_VIEW")).thenReturn(Optional.of(PermissionScope.STORE));
        when(stores.findById(a)).thenReturn(Optional.of(Store.builder().id(a).active(true).build()));
        assertThat(service.require(actor,null,"INVENTORY_VIEW")).isEqualTo(a);
    }
}
