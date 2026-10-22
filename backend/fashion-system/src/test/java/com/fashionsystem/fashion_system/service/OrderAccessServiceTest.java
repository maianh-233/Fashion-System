package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderAccessServiceTest {
    final UserScopeService scopes = mock(UserScopeService.class);
    final AuthorizationService permissions = mock(AuthorizationService.class);
    final StoreRepository stores = mock(StoreRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final OrderAccessService service = new OrderAccessService(
            new StoreAccessService(scopes, permissions, stores), scopes, orders);
    final UUID actor = UUID.randomUUID(), handler = UUID.randomUUID();
    final UUID storeA = UUID.randomUUID(), storeB = UUID.randomUUID(), orderId = UUID.randomUUID();

    void storeStaffOf(UUID store) {
        when(scopes.resolve(actor)).thenReturn(new UserScope(UserScope.Kind.STORE, store, "S", "S"));
        when(stores.findById(store)).thenReturn(Optional.of(Store.builder().id(store).active(true).build()));
    }

    void grant(String permission) {
        when(permissions.getPermissionScope(actor, permission)).thenReturn(Optional.of(PermissionScope.ALL));
    }

    Order orderAt(UUID store) {
        Order order = Order.builder().id(orderId).storeId(store).handlerUserId(handler)
                .orderType(OrderType.ONLINE).status(OrderStatus.DELIVERY_FAILED).build();
        when(orders.findById(orderId)).thenReturn(Optional.of(order));
        return order;
    }

    @Test void storeStaffCannotOpenOrderOfAnotherStore() {
        storeStaffOf(storeA);
        grant(OrderPermissions.VIEW);
        orderAt(storeB);
        assertThatThrownBy(() -> service.requireOrder(actor, orderId, OrderPermissions.VIEW))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }

    @Test void storeStaffAlwaysWorksOnOwnStore() {
        storeStaffOf(storeA);
        grant(OrderPermissions.CREATE_OFFLINE);
        assertThat(service.resolveStore(actor, null, OrderPermissions.CREATE_OFFLINE)).isEqualTo(storeA);
        assertThatThrownBy(() -> service.resolveStore(actor, storeB, OrderPermissions.CREATE_OFFLINE))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }

    @Test void parentOrderOfSplitOrderIsNotHandledDirectly() {
        storeStaffOf(storeA);
        grant(OrderPermissions.VIEW);
        orderAt(null);
        assertThatThrownBy(() -> service.requireOrder(actor, orderId, OrderPermissions.VIEW))
                .isInstanceOf(BusinessException.class);
    }

    @Test void onlyHandlerConfirmsReturnWithoutOverride() {
        storeStaffOf(storeA);
        grant(OrderPermissions.VIEW);
        Order order = orderAt(storeA);
        assertThatThrownBy(() -> service.requireReturnConfirmer(actor, order))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));

        order.setHandlerUserId(actor);
        assertThatCode(() -> service.requireReturnConfirmer(actor, order)).doesNotThrowAnyException();
    }

    @Test void overridePermissionLetsManagerConfirmReturnOfOtherHandler() {
        storeStaffOf(storeA);
        grant(OrderPermissions.VIEW);
        grant(OrderPermissions.RETURN_OVERRIDE);
        Order order = orderAt(storeA);
        assertThatCode(() -> service.requireReturnConfirmer(actor, order)).doesNotThrowAnyException();
    }
}
