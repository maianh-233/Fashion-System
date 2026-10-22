package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.notification.PermissionRecipientResolver;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderNotificationTest {
    final OrderNotificationComposer composer = new OrderNotificationComposer();
    final NotificationPublisher publisher = mock(NotificationPublisher.class);
    final PermissionRecipientResolver recipients = mock(PermissionRecipientResolver.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final StoreRepository stores = mock(StoreRepository.class);
    // Executor đồng bộ để kiểm tra kết quả ngay.
    final OrderNotificationListener listener = new OrderNotificationListener(composer, publisher, recipients, orders,
            stores, Runnable::run);
    final UUID orderId = UUID.randomUUID(), storeId = UUID.randomUUID(), customerId = UUID.randomUUID();
    final UUID staffId = UUID.randomUUID(), handlerId = UUID.randomUUID();

    @BeforeEach void setUp() {
        when(stores.findById(storeId)).thenReturn(Optional.of(Store.builder().id(storeId).name("Lunaria Q1").build()));
        when(recipients.resolveStoreStaff(OrderPermissions.CONFIRM, storeId)).thenReturn(Set.of(RealtimeIdentity.user(staffId)));
    }

    OrderEvents.StatusChanged event(OrderStatus from, OrderStatus to, OrderActor actor, String reason, UUID handler) {
        return new OrderEvents.StatusChanged(orderId, "LB-1", OrderType.ONLINE, storeId, customerId, null, handler,
                from, to, actor, reason);
    }

    List<NotificationRequest> sent() {
        var captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(publisher, atLeast(0)).publish(captor.capture());
        return captor.getAllValues();
    }

    @Test void staffConfirmationNotifiesOnlyTheCustomerWithLink() {
        listener.onStatusChanged(event(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderActor.user(staffId), null, staffId));
        var requests = sent();
        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().recipientIdentities()).containsExactly(RealtimeIdentity.customer(customerId));
        assertThat(requests.getFirst().title()).isEqualTo("Đơn LB-1: Đã xác nhận");
        assertThat(requests.getFirst().message()).contains("Lunaria Q1");
        assertThat(requests.getFirst().actionUrl()).isEqualTo("/orderdetail?id=" + orderId);
    }

    @Test void staffCancellationSendsReasonToCustomer() {
        listener.onStatusChanged(event(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderActor.user(staffId), "Hết hàng", staffId));
        assertThat(sent()).singleElement().satisfies(r -> {
            assertThat(r.type()).isEqualTo(NotificationType.ORDER_CANCELLED);
            assertThat(r.message()).contains("Lý do: Hết hàng");
        });
    }

    @Test void customerCancellationNotifiesStoreStaffButNotTheCustomer() {
        listener.onStatusChanged(event(OrderStatus.PENDING, OrderStatus.CANCELLED, OrderActor.customer(customerId), "Đổi ý", null));
        assertThat(sent()).singleElement().satisfies(r -> {
            assertThat(r.recipientIdentities()).containsExactly(RealtimeIdentity.user(staffId));
            assertThat(r.type()).isEqualTo(NotificationType.ORDER_CANCELLED_BY_CUSTOMER);
            assertThat(r.actionUrl()).isEqualTo("/admin/orders?order=" + orderId);
        });
    }

    @Test void customerCancellationGoesToHandlerWhenOrderHasOne() {
        listener.onStatusChanged(event(OrderStatus.PENDING, OrderStatus.CANCELLED, OrderActor.customer(customerId), null, handlerId));
        assertThat(sent()).singleElement().satisfies(r ->
                assertThat(r.recipientIdentities()).containsExactly(RealtimeIdentity.user(handlerId)));
    }

    @Test void shipperFallbackToConfirmedIsNotSentToCustomer() {
        listener.onStatusChanged(event(OrderStatus.HANDED_TO_SHIPPER, OrderStatus.CONFIRMED, OrderActor.shipper(), "Không tìm được tài xế", handlerId));
        assertThat(sent()).isEmpty();
    }

    @Test void childOrderNotificationUsesCustomerFacingParentCode() {
        UUID parentId = UUID.randomUUID();
        when(orders.findById(parentId)).thenReturn(Optional.of(Order.builder().id(parentId).orderCode("LB-PARENT").build()));
        listener.onStatusChanged(new OrderEvents.StatusChanged(orderId, "LB-CHILD", OrderType.ONLINE, storeId, customerId,
                parentId, staffId, OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderActor.user(staffId), null));
        assertThat(sent()).singleElement().satisfies(r -> {
            assertThat(r.title()).startsWith("Đơn LB-PARENT");
            assertThat(r.message()).isEqualTo("Cửa hàng Lunaria Q1 đã xác nhận phần đơn của bạn và đang chuẩn bị hàng.");
            assertThat(r.entityId()).isEqualTo(parentId);
        });
    }

    @Test void offlineOrdersDoNotNotify() {
        listener.onStatusChanged(new OrderEvents.StatusChanged(orderId, "LB-1", OrderType.OFFLINE, storeId, customerId,
                null, null, OrderStatus.COMPLETED, OrderStatus.RETURNED, OrderActor.user(staffId), null));
        verifyNoInteractions(publisher);
    }

    @Test void splitOrderTellsCustomerEachPartAndEachStoreItsPart() {
        UUID otherStore = UUID.randomUUID(), otherStaff = UUID.randomUUID();
        when(recipients.resolveStoreStaff(OrderPermissions.CONFIRM, otherStore)).thenReturn(Set.of(RealtimeIdentity.user(otherStaff)));
        listener.onOnlineOrderPlaced(new OrderEvents.OnlineOrderPlaced(orderId, "LB-10", customerId, true, List.of(
                new OrderEvents.OnlineOrderPlaced.Part(UUID.randomUUID(), "LB-11", storeId, "Lunaria Q1", List.of("Áo (Đen/M) × 2")),
                new OrderEvents.OnlineOrderPlaced.Part(UUID.randomUUID(), "LB-12", otherStore, "Lunaria Thủ Đức", List.of("Quần (30) × 1")))));
        var requests = sent();
        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).recipientIdentities()).containsExactly(RealtimeIdentity.user(staffId));
        assertThat(requests.get(0).message()).contains("Áo (Đen/M) × 2").contains("một phần của đơn LB-10");
        assertThat(requests.get(1).recipientIdentities()).containsExactly(RealtimeIdentity.user(otherStaff));
        var customer = requests.get(2);
        assertThat(customer.type()).isEqualTo(NotificationType.ORDER_SPLIT);
        assertThat(customer.title()).isEqualTo("Đơn LB-10 được giao thành 2 phần");
        assertThat(customer.message()).contains("Phần 1 giao từ Lunaria Q1: Áo (Đen/M) × 2")
                .contains("Phần 2 giao từ Lunaria Thủ Đức: Quần (30) × 1");
    }

    @Test void deliveryFailureDoesNotBreakTheListener() {
        doThrow(new IllegalStateException("kafka down")).when(publisher).publish(any());
        listener.onStatusChanged(event(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderActor.user(staffId), null, staffId));
        // Không ném lỗi ra ngoài: thao tác của nhân viên đã commit thành công.
    }

    @Test void failedShipmentAlertsHandlerWithNextStep() {
        listener.onShipmentFailed(new OrderEvents.ShipmentFailed(orderId, "LB-1", storeId, handlerId, 1,
                "Lalamove không tìm được tài xế", false));
        assertThat(sent()).singleElement().satisfies(r -> {
            assertThat(r.recipientIdentities()).containsExactly(RealtimeIdentity.user(handlerId));
            assertThat(r.type()).isEqualTo(NotificationType.ORDER_SHIPMENT_FAILED);
            assertThat(r.message()).startsWith("Lalamove không tìm được tài xế.").contains("gửi lại yêu cầu giao hàng");
        });
    }
}
