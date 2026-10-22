package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.PermissionRecipientResolver;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Biến sự kiện đơn hàng thành thông báo trong hệ thống (lưu DB + đẩy STOMP qua đường thông báo hiện có).
 * Chạy sau khi transaction commit và trên luồng nền; lỗi gửi chỉ ghi log, không ảnh hưởng nghiệp vụ.
 */
@Slf4j
@Component
public class OrderNotificationListener {
    /** Quyền dùng để tìm nhân viên cửa hàng cần xử lý đơn online. */
    static final String STAFF_PERMISSION = OrderPermissions.CONFIRM;

    private final OrderNotificationComposer composer;
    private final NotificationPublisher publisher;
    private final PermissionRecipientResolver recipients;
    private final OrderRepository orders;
    private final StoreRepository stores;
    private final TaskExecutor executor;

    public OrderNotificationListener(OrderNotificationComposer composer, NotificationPublisher publisher,
                                     PermissionRecipientResolver recipients, OrderRepository orders,
                                     StoreRepository stores,
                                     @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.composer = composer;
        this.publisher = publisher;
        this.recipients = recipients;
        this.orders = orders;
        this.stores = stores;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(OrderEvents.StatusChanged event) {
        if (event.orderType() != OrderType.ONLINE || event.storeId() == null) return;
        executor.execute(() -> safely("status " + event.orderCode(), () -> statusChanged(event)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOnlineOrderPlaced(OrderEvents.OnlineOrderPlaced event) {
        executor.execute(() -> safely("placed " + event.orderCode(), () -> placed(event)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentFailed(OrderEvents.ShipmentFailed event) {
        executor.execute(() -> safely("shipment failed " + event.orderCode(), () -> send(
                event.handlerUserId() != null ? Set.of(RealtimeIdentity.user(event.handlerUserId()))
                        : recipients.resolveStoreStaff(STAFF_PERMISSION, event.storeId()),
                null, composer.shipmentFailed(event), event.orderId(), "/admin/orders?order=" + event.orderId(),
                event.orderCode())));
    }

    void statusChanged(OrderEvents.StatusChanged event) {
        Order parent = event.parentOrderId() == null ? null : orders.findById(event.parentOrderId()).orElse(null);
        UUID customerOrderId = parent == null ? event.orderId() : parent.getId();
        String customerOrderCode = parent == null ? event.orderCode() : parent.getOrderCode();
        String storeName = stores.findById(event.storeId()).map(Store::getName).orElse("");
        var context = new OrderNotificationComposer.StatusContext(event, customerOrderCode,
                parent == null ? null : event.orderCode(), storeName);
        UUID actorUser = event.actor().type() == OrderActorType.USER ? event.actor().id() : null;

        if (event.customerId() != null)
            composer.forCustomer(context).ifPresent(message -> send(Set.of(RealtimeIdentity.customer(event.customerId())),
                    actorUser, message, customerOrderId, "/orderdetail?id=" + customerOrderId, customerOrderCode));
        // Đơn trả qua cổng chưa thanh toán chưa từng hiện cho cửa hàng: không báo nhân viên.
        boolean hiddenFromStore = orders.findById(event.orderId())
                .map(order -> OrderPaymentStatus.hiddenFromStore(order.getPaymentStatus())).orElse(false);
        if (!hiddenFromStore) composer.forStaff(context).ifPresent(message -> send(
                event.handlerUserId() != null ? Set.of(RealtimeIdentity.user(event.handlerUserId()))
                        : recipients.resolveStoreStaff(STAFF_PERMISSION, event.storeId()),
                null, message, event.orderId(), "/admin/orders?order=" + event.orderId(), event.orderCode()));
    }

    void placed(OrderEvents.OnlineOrderPlaced event) {
        for (var part : event.parts())
            send(recipients.resolveStoreStaff(STAFF_PERMISSION, part.storeId()), null,
                    composer.newOrderForStaff(part, event.split(), event.orderCode()), part.orderId(),
                    "/admin/orders?order=" + part.orderId(), part.orderCode());
        send(Set.of(RealtimeIdentity.customer(event.customerId())), null, composer.placedForCustomer(event),
                event.orderId(), "/orderdetail?id=" + event.orderId(), event.orderCode());
    }

    private void send(Set<RealtimeIdentity> to, UUID actorUser, OrderNotificationComposer.Message message, UUID orderId,
                      String actionUrl, String orderCode) {
        if (to.isEmpty()) {
            log.warn("No recipient for order notification {} of {}", message.type(), orderCode);
            return;
        }
        publisher.publish(new NotificationRequest(List.copyOf(to), actorUser, message.type(), message.title(),
                message.body(), "ORDER", orderId, actionUrl, Map.of("orderCode", orderCode)));
    }

    private static void safely(String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException failure) {
            log.warn("Order notification failed: {}", what, failure);
        }
    }
}
