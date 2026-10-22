package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.OrderStatusHistoryMapper;
import com.fashionsystem.fashion_system.repository.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderAdminServiceTest {
    final OrderAccessService access = mock(OrderAccessService.class);
    final OrderAdminQueryRepository queries = mock(OrderAdminQueryRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final OrderStatusHistoryRepository histories = mock(OrderStatusHistoryRepository.class);
    final OrderAdminService service = new OrderAdminService(access,
            new OrderStatusService(orders, histories, new OrderStateMachine(),
                    mock(org.springframework.context.ApplicationEventPublisher.class)),
            new OrderStateMachine(), new OrderStatusHistoryMapper(),
            mock(UserScopeService.class), queries, orders, mock(OrderItemRepository.class),
            mock(OrderAddressRepository.class), mock(OrderPromotionRepository.class), mock(PromotionRepository.class),
            mock(PaymentRepository.class), mock(PaymentTransactionRepository.class), mock(ShipmentRepository.class),
            mock(RefundRepository.class), mock(OrderChatRoomRepository.class),
            mock(CustomerRepository.class), mock(CustomerAddressRepository.class), mock(StoreRepository.class),
            mock(UserRepository.class));
    final UUID actor = UUID.randomUUID(), store = UUID.randomUUID(), orderId = UUID.randomUUID();

    Order order(OrderStatus status, boolean active) {
        Order order = Order.builder().id(orderId).storeId(store).orderCode("LB-1").orderType(OrderType.OFFLINE)
                .status(status).active(active).build();
        when(access.requireOrderForUpdate(eq(actor), eq(orderId), anyString())).thenReturn(order);
        return order;
    }

    @BeforeEach void setUp() {
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void hideMarksFinishedOrderInactiveAndRecordsWho() {
        Order order = order(OrderStatus.COMPLETED, true);
        var detail = service.hide(actor, orderId);
        assertThat(order.getActive()).isFalse();
        assertThat(order.getHiddenBy()).isEqualTo(actor);
        assertThat(order.getHiddenAt()).isNotNull();
        assertThat(detail.visibility().active()).isFalse();
        verify(access).requireOrderForUpdate(actor, orderId, OrderPermissions.HIDE);
    }

    @Test void onlyTerminalOrdersCanBeHidden() {
        for (OrderStatus status : List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.HANDED_TO_SHIPPER,
                OrderStatus.SHIPPING, OrderStatus.DELIVERY_FAILED)) {
            Order order = order(status, true);
            assertThatThrownBy(() -> service.hide(actor, orderId))
                    .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
            assertThat(order.getActive()).isTrue();
        }
    }

    @Test void hidingTwiceOrRestoringVisibleOrderIsRejected() {
        order(OrderStatus.CANCELLED, false);
        assertThatThrownBy(() -> service.hide(actor, orderId)).isInstanceOf(BusinessException.class);
        order(OrderStatus.CANCELLED, true);
        assertThatThrownBy(() -> service.restore(actor, orderId)).isInstanceOf(BusinessException.class);
    }

    @Test void restoreBringsOrderBackAndRecordsWho() {
        Order order = order(OrderStatus.RETURNED, false);
        service.restore(actor, orderId);
        assertThat(order.getActive()).isTrue();
        assertThat(order.getRestoredBy()).isEqualTo(actor);
        verify(access).requireOrderForUpdate(actor, orderId, OrderPermissions.RESTORE);
    }

    @Test void actionsDependOnStatusVisibilityAndPermission() {
        Order finished = Order.builder().id(orderId).storeId(store).status(OrderStatus.COMPLETED).active(true).build();
        when(access.can(actor, finished, OrderPermissions.HIDE)).thenReturn(true);
        assertThat(service.actions(actor, finished)).containsExactly(OrderAdminService.ACTION_HIDE);

        Order pending = Order.builder().id(orderId).storeId(store).status(OrderStatus.PENDING).active(true).build();
        when(access.can(eq(actor), eq(pending), anyString())).thenReturn(true);
        assertThat(service.actions(actor, pending)).isEmpty();

        Order hidden = Order.builder().id(orderId).storeId(store).status(OrderStatus.CANCELLED).active(false).build();
        assertThat(service.actions(actor, hidden)).isEmpty();
        when(access.can(actor, hidden, OrderPermissions.RESTORE)).thenReturn(true);
        assertThat(service.actions(actor, hidden)).containsExactly(OrderAdminService.ACTION_RESTORE);
    }

    @Test void listUsesResolvedStoreWholeDayRangeAndDefaultNewestFirst() {
        when(access.resolveStore(actor, null, OrderPermissions.VIEW)).thenReturn(store);
        when(queries.search(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        service.list(actor, new OrderAdminService.ListQuery(null, OrderType.ONLINE, true, null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 14), "0909", null, ""), PageRequest.of(0, 10));
        verify(queries).search(argThat(f -> f.storeId().equals(store) && !f.active()
                        && f.to().equals(LocalDate.of(2026, 10, 15).atStartOfDay())),
                argThat(p -> p.getSort().equals(Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Test void listRejectsUnknownSortField() {
        when(access.resolveStore(any(), any(), any())).thenReturn(store);
        assertThatThrownBy(() -> service.list(actor, new OrderAdminService.ListQuery(null, OrderType.OFFLINE, false,
                null, null, null, "", null, ""), PageRequest.of(0, 10, Sort.by("cashReceived"))))
                .isInstanceOf(BusinessException.class);
    }
}
