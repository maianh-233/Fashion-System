package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.OrderStatusHistoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderStatusServiceTest {
    final OrderRepository orders = mock(OrderRepository.class);
    final OrderStatusHistoryRepository histories = mock(OrderStatusHistoryRepository.class);
    final org.springframework.context.ApplicationEventPublisher events =
            mock(org.springframework.context.ApplicationEventPublisher.class);
    final OrderStatusService service = new OrderStatusService(orders, histories, new OrderStateMachine(), events);
    final UUID orderId = UUID.randomUUID(), staff = UUID.randomUUID();

    Order order(OrderStatus status) {
        Order order = Order.builder().id(orderId).orderType(OrderType.ONLINE).status(status).build();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        return order;
    }

    @Test void transitionUpdatesStatusAndWritesHistoryWithActor() {
        order(OrderStatus.PENDING);
        Order saved = service.transition(orderId, OrderStatus.CONFIRMED, OrderActor.user(staff), null);

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        var history = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(histories).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.getValue().getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(history.getValue().getActorType()).isEqualTo(OrderActorType.USER);
        assertThat(history.getValue().getChangedBy()).isEqualTo(staff);
    }

    @Test void invalidTransitionChangesNothing() {
        Order order = order(OrderStatus.SHIPPING);
        assertThatThrownBy(() -> service.transition(orderId, OrderStatus.CANCELLED, OrderActor.user(staff), "Khách đổi ý"))
                .isInstanceOf(BusinessException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        verify(orders, never()).save(any());
        verify(histories, never()).save(any());
    }

    @Test void staffCancellationRequiresReasonAndStoresIt() {
        order(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> service.transition(orderId, OrderStatus.CANCELLED, OrderActor.user(staff), "  "))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));

        Order saved = service.transition(orderId, OrderStatus.CANCELLED, OrderActor.user(staff), " Hết màu khách chọn ");
        assertThat(saved.getCancelReason()).isEqualTo("Hết màu khách chọn");
        assertThat(saved.getCancelledByType()).isEqualTo(OrderActorType.USER);
    }

    @Test void customerMayCancelPendingOrderWithoutReason() {
        order(OrderStatus.PENDING);
        Order saved = service.transition(orderId, OrderStatus.CANCELLED, OrderActor.customer(UUID.randomUUID()), null);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(saved.getCancelledByType()).isEqualTo(OrderActorType.CUSTOMER);
    }

    @Test void initializeUsesOrderTypeAndRecordCreatedWritesFirstHistoryRow() {
        Order offline = Order.builder().orderType(OrderType.OFFLINE).build();
        service.initialize(offline);
        assertThat(offline.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        offline.setId(orderId);
        service.recordCreated(offline, OrderActor.user(staff), "Tạo đơn hàng");
        var history = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(histories).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isNull();
        assertThat(history.getValue().getToStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test void everyTransitionPublishesStatusChangedEvent() {
        order(OrderStatus.PENDING);
        service.transition(orderId, OrderStatus.CONFIRMED, OrderActor.user(staff), null);
        verify(events).publishEvent(argThat((Object e) -> e instanceof OrderEvents.StatusChanged changed
                && changed.from() == OrderStatus.PENDING && changed.to() == OrderStatus.CONFIRMED));
    }

    Order parentWithChildren(OrderStatus... childStatuses) {
        UUID parentId = UUID.randomUUID();
        Order parent = Order.builder().id(parentId).orderType(OrderType.ONLINE).status(OrderStatus.PENDING).build();
        when(orders.findByIdForUpdate(parentId)).thenReturn(Optional.of(parent));
        when(orders.findAllByParentOrderId(parentId)).thenReturn(java.util.Arrays.stream(childStatuses)
                .map(s -> Order.builder().id(UUID.randomUUID()).status(s).build()).toList());
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        return parent;
    }

    @Test void parentOfSplitOrderFollowsChildrenOnlyWhenAllFinished() {
        Order inProgress = parentWithChildren(OrderStatus.COMPLETED, OrderStatus.SHIPPING);
        service.syncParent(inProgress.getId());
        assertThat(inProgress.getStatus()).isEqualTo(OrderStatus.PENDING);

        Order cancelled = parentWithChildren(OrderStatus.CANCELLED, OrderStatus.CANCELLED);
        service.syncParent(cancelled.getId());
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order partlyDelivered = parentWithChildren(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        service.syncParent(partlyDelivered.getId());
        assertThat(partlyDelivered.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        Order returned = parentWithChildren(OrderStatus.RETURNED, OrderStatus.CANCELLED);
        service.syncParent(returned.getId());
        assertThat(returned.getStatus()).isEqualTo(OrderStatus.RETURNED);
    }
}
