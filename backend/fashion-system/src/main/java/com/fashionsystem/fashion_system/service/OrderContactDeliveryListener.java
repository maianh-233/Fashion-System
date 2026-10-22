package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderCustomerContact;
import com.fashionsystem.fashion_system.entity.OrderCustomerContact.DeliveryStatus;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.OrderCustomerContactRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gửi nội dung "Liên hệ khách" sau khi lần liên hệ đã được lưu (AFTER_COMMIT): email qua SMTP và/hoặc
 * thông báo trong hệ thống qua đường thông báo hiện có. Việc gửi chạy trên luồng nền để SMTP/Kafka chậm
 * không giữ request của nhân viên; kết quả từng kênh được ghi lại trong transaction riêng.
 */
@Slf4j
@Component
public class OrderContactDeliveryListener {
    private final OrderCustomerContactRepository contacts;
    private final OrderRepository orders;
    private final MailHelper mail;
    private final NotificationPublisher notifications;
    private final TransactionTemplate transaction;
    private final TaskExecutor executor;

    public OrderContactDeliveryListener(OrderCustomerContactRepository contacts, OrderRepository orders, MailHelper mail,
                                        NotificationPublisher notifications, PlatformTransactionManager transactionManager,
                                        @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.executor = executor;
        this.contacts = contacts;
        this.orders = orders;
        this.mail = mail;
        this.notifications = notifications;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContactRequested(OrderEvents.CustomerContactRequested event) {
        executor.execute(() -> deliver(event));
    }

    void deliver(OrderEvents.CustomerContactRequested event) {
        OrderCustomerContact contact = contacts.findById(event.contactId()).orElse(null);
        if (contact == null) return;
        Order order = orders.findById(contact.getOrderId()).orElse(null);
        String code = order == null ? "" : order.getOrderCode();

        if (contact.getEmailStatus() == DeliveryStatus.PENDING) {
            try {
                mail.sendText(contact.getEmailTo(), contact.getSubject(),
                        contact.getContent() + "\n\n— Lunaria Boutique · Đơn hàng " + code);
                contact.setEmailStatus(DeliveryStatus.SENT);
            } catch (RuntimeException failure) {
                log.warn("Order contact email {} failed", contact.getId(), failure);
                contact.setEmailStatus(DeliveryStatus.FAILED);
                contact.setEmailError(failure.getMessage());
            }
        }
        if (contact.getNotificationStatus() == DeliveryStatus.PENDING) {
            try {
                // Khách xem đơn của mình qua đơn tổng nếu đơn này là một phần của đơn bị tách.
                var target = order != null && order.getParentOrderId() != null ? order.getParentOrderId() : contact.getOrderId();
                notifications.publish(new NotificationRequest(List.of(RealtimeIdentity.customer(contact.getCustomerId())),
                        contact.getSentBy(), NotificationType.ORDER_CUSTOMER_CONTACT, contact.getSubject(),
                        contact.getContent(), "ORDER", target, "/orderdetail?id=" + target, Map.of("orderCode", code)));
                contact.setNotificationStatus(DeliveryStatus.SENT);
            } catch (RuntimeException failure) {
                log.warn("Order contact notification {} failed", contact.getId(), failure);
                contact.setNotificationStatus(DeliveryStatus.FAILED);
            }
        }
        transaction.executeWithoutResult(status -> contacts.save(contact));
    }
}
