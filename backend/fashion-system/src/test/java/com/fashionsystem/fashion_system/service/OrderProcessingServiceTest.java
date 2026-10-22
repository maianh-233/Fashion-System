package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.OrderActionRequests;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderProcessingServiceTest {
    final OrderAccessService access = mock(OrderAccessService.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final OrderStatusService statuses = new OrderStatusService(orders, mock(OrderStatusHistoryRepository.class),
            new OrderStateMachine(), mock(ApplicationEventPublisher.class));
    final OnlineOrderService onlineOrders = mock(OnlineOrderService.class);
    final OrderAdminService admin = mock(OrderAdminService.class);
    final CustomerRepository customers = mock(CustomerRepository.class);
    final OrderCustomerContactRepository contacts = mock(OrderCustomerContactRepository.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final RefundRepository refunds = mock(RefundRepository.class);
    final OrderProcessingService service = new OrderProcessingService(access, statuses, onlineOrders, admin, orders,
            customers, contacts, mock(UserRepository.class), payments, refunds, new OrderAmountCalculator(orders), events);
    final UUID actor = UUID.randomUUID(), orderId = UUID.randomUUID(), customerId = UUID.randomUUID();

    Order order(OrderStatus status, String permission) {
        Order order = Order.builder().id(orderId).storeId(UUID.randomUUID()).orderType(OrderType.ONLINE)
                .customerId(customerId).status(status).paymentMethod("COD").paymentStatus("UNPAID").build();
        when(access.requireOrderForUpdate(actor, orderId, permission)).thenReturn(order);
        return order;
    }

    @BeforeEach void setUp() {
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void confirmMakesActorTheHandler() {
        Order order = order(OrderStatus.PENDING, OrderPermissions.CONFIRM);
        service.confirm(actor, orderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getHandlerUserId()).isEqualTo(actor);
    }

    @Test void cancelRestocksAndKeepsReason() {
        Order order = order(OrderStatus.CONFIRMED, OrderPermissions.CANCEL);
        service.cancel(actor, orderId, "Hết size khách đặt");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("Hết size khách đặt");
        verify(onlineOrders).restock(order, actor);
    }

    @Test void gatewayPaidOrderCannotBeCancelled() {
        Order order = order(OrderStatus.PENDING, OrderPermissions.CANCEL);
        when(admin.isGatewayPaid(order)).thenReturn(true);
        assertThatThrownBy(() -> service.cancel(actor, orderId, "Khách đổi ý"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã thanh toán");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verifyNoInteractions(onlineOrders);
    }

    @Test void cancelAfterHandoverIsRejectedByStateMachine() {
        Order order = order(OrderStatus.HANDED_TO_SHIPPER, OrderPermissions.CANCEL);
        assertThatThrownBy(() -> service.cancel(actor, orderId, "Khách đổi ý")).isInstanceOf(BusinessException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.HANDED_TO_SHIPPER);
        verifyNoInteractions(onlineOrders);
    }

    @Test void confirmReturnChecksHandlerRuleThenRestocks() {
        Order order = order(OrderStatus.DELIVERY_FAILED, OrderPermissions.VIEW);
        service.confirmReturn(actor, orderId);
        verify(access).requireReturnConfirmer(actor, order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
        verify(onlineOrders).restock(order, actor);
    }

    @Test void contactNeedsAChannelAndEmailForEmailChannel() {
        assertThatThrownBy(() -> service.contact(actor, orderId, new OrderActionRequests.Contact("Hỏi", "Nội dung", false, false)))
                .isInstanceOf(BusinessException.class);
        Order order = Order.builder().id(orderId).customerId(customerId).build();
        when(access.requireOrder(actor, orderId, OrderPermissions.CONTACT_CUSTOMER)).thenReturn(order);
        when(customers.findById(customerId)).thenReturn(Optional.of(Customer.builder().id(customerId).email(" ").build()));
        assertThatThrownBy(() -> service.contact(actor, orderId, new OrderActionRequests.Contact("Hỏi", "Nội dung", true, false)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("email");
        verifyNoInteractions(contacts, events);
    }

    @Test void contactIsSavedAsPendingAndDeliveredAfterCommit() {
        Order order = Order.builder().id(orderId).customerId(customerId).build();
        when(access.requireOrder(actor, orderId, OrderPermissions.CONTACT_CUSTOMER)).thenReturn(order);
        when(customers.findById(customerId)).thenReturn(Optional.of(Customer.builder().id(customerId).email("a@b.vn").build()));
        when(contacts.save(any())).thenAnswer(call -> { OrderCustomerContact c = call.getArgument(0); c.setId(UUID.randomUUID()); return c; });

        var view = service.contact(actor, orderId, new OrderActionRequests.Contact("Hết tài xế", "Anh/chị muốn chờ hay hủy?", true, true));
        assertThat(view.emailStatus()).isEqualTo(OrderCustomerContact.DeliveryStatus.PENDING);
        assertThat(view.notificationStatus()).isEqualTo(OrderCustomerContact.DeliveryStatus.PENDING);
        verify(events).publishEvent(any(OrderEvents.CustomerContactRequested.class));
    }

    @Test void gatewayPaidOrderCanBeCancelledAfterFailedDeliveryWithPendingRefund() {
        Order order = order(OrderStatus.CONFIRMED, OrderPermissions.CANCEL);
        order.setOrderCode("LB-9");
        order.setTotalAmount(java.math.BigDecimal.valueOf(938_000));
        when(admin.isGatewayPaid(order)).thenReturn(true);
        when(admin.hasFailedDelivery(order)).thenReturn(true);
        UUID paymentId = UUID.randomUUID();
        when(payments.findAllByOrderIdOrderByCreatedAtAsc(orderId)).thenReturn(java.util.List.of(
                Payment.builder().id(paymentId).orderId(orderId).status("SUCCESS").method("VNPAY").build()));

        service.cancel(actor, orderId, "Khách không muốn chờ tài xế");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(onlineOrders).restock(order, actor);
        verify(refunds).save(argThat(r -> r.getPaymentId().equals(paymentId) && r.getOrderId().equals(orderId)
                && "PENDING".equals(r.getStatus()) && r.getAmount().compareTo(java.math.BigDecimal.valueOf(938_000)) == 0
                && r.getReason().contains("Khách không muốn chờ tài xế")));
    }

    @Test void unpaidOrderCancellationCreatesNoRefund() {
        order(OrderStatus.CONFIRMED, OrderPermissions.CANCEL);
        service.cancel(actor, orderId, "Hết hàng");
        verifyNoInteractions(refunds);
    }
}
