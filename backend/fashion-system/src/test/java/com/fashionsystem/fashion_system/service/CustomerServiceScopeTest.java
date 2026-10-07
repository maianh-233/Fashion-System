package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceScopeTest {
    private UserScopeService scopes;
    private AuthorizationService authorization;
    private CustomerAccessService access;
    private UUID actor;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        scopes = mock(UserScopeService.class);
        authorization = mock(AuthorizationService.class);
        access = new CustomerAccessService(scopes, authorization);
        actor = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    void storeStaffListIsForcedToAssignedStore() {
        when(scopes.resolve(actor)).thenReturn(UserScope.store(
                Store.builder().id(storeId).code("HCM01").name("HCM 01").build()));
        when(authorization.hasPermission(actor, "CUSTOMER_VIEW", PermissionScope.STORE)).thenReturn(true);

        CustomerAccessService.QueryScope result = access.resolveList(actor, null, false);

        assertThat(result.storeId()).isEqualTo(storeId);
        assertThat(result.noStore()).isFalse();
    }

    @Test
    void storeStaffCannotSubmitAnotherStoreOrNoStoreFilter() {
        when(scopes.resolve(actor)).thenReturn(UserScope.store(
                Store.builder().id(storeId).code("HCM01").name("HCM 01").build()));
        when(authorization.hasPermission(actor, "CUSTOMER_VIEW", PermissionScope.STORE)).thenReturn(true);

        assertThatThrownBy(() -> access.resolveList(actor, UUID.randomUUID(), false))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> access.resolveList(actor, null, true))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void globalStaffCanSelectNoStoreWebsiteCustomers() {
        when(scopes.resolve(actor)).thenReturn(UserScope.global());
        when(authorization.hasPermission(actor, "CUSTOMER_VIEW", PermissionScope.ALL)).thenReturn(true);

        CustomerAccessService.QueryScope result = access.resolveList(actor, null, true);

        assertThat(result.storeId()).isNull();
        assertThat(result.noStore()).isTrue();
    }

    @Test
    void storeStaffCannotEditOtherStoreOrWebsiteCustomer() {
        when(scopes.resolve(actor)).thenReturn(UserScope.store(
                Store.builder().id(storeId).code("HCM01").name("HCM 01").build()));
        when(authorization.hasPermission(actor, "CUSTOMER_UPDATE", PermissionScope.STORE)).thenReturn(true);
        Customer otherStore = Customer.builder().source(CustomerSource.STORE)
                .originStoreId(UUID.randomUUID()).build();
        Customer website = Customer.builder().source(CustomerSource.WEBSITE).originStoreId(null).build();

        assertThatThrownBy(() -> access.requireEdit(actor, otherStore)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> access.requireEdit(actor, website)).isInstanceOf(BusinessException.class);
    }
}

