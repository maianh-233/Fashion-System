package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.PermissionRecipientRepository;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Thông báo trong app của xếp ca: lịch được công bố / thay đổi (nhân viên), yêu cầu đổi ca (đồng nghiệp, quản lý cửa
 * hàng), kết quả duyệt. Gửi sau commit trên luồng nền để không chặn khi Kafka chậm.
 */
@Slf4j
@Component
public class ShiftNotifier {
    static final String TYPE = "SHIFT_SCHEDULE";
    static final String MY_SHIFTS = "/admin/me/shifts";
    static final String SWAP_ADMIN = "/admin/attendance/swaps";

    private final NotificationPublisher publisher;
    private final PermissionRecipientRepository recipients;
    private final TaskExecutor delivery;

    public ShiftNotifier(NotificationPublisher publisher, PermissionRecipientRepository recipients,
                         @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.publisher = publisher;
        this.recipients = recipients;
        this.delivery = delivery;
    }

    void toUser(UUID userId, UUID actor, String title, String message, UUID entityId) {
        send(List.of(userId), actor, title, message, "SHIFT_SCHEDULE", entityId, MY_SHIFTS);
    }

    void toUsers(Collection<UUID> userIds, UUID actor, String title, String message, String entityType, UUID entityId) {
        send(userIds, actor, title, message, entityType, entityId, MY_SHIFTS);
    }

    /** Người được xếp ca / duyệt đổi ca tại cửa hàng (quản lý cửa hàng; nếu không có thì HR cấp chuỗi). */
    void toStoreManagers(UUID storeId, UUID actor, String title, String message, UUID swapId) {
        List<UUID> managers = recipients.findStoreOnlyUserIds("SHIFT_MANAGE", storeId);
        if (managers.isEmpty()) managers = recipients.findGlobalEligibleUserIds("SHIFT_MANAGE");
        send(managers, actor, title, message, "SHIFT_SWAP", swapId, SWAP_ADMIN);
    }

    private void send(Collection<UUID> userIds, UUID actor, String title, String message, String entityType, UUID entityId,
                      String link) {
        List<RealtimeIdentity> to = userIds.stream().filter(Objects::nonNull).filter(id -> !id.equals(actor)).distinct()
                .map(RealtimeIdentity::user).toList();
        if (to.isEmpty()) return;
        NotificationRequest request = new NotificationRequest(to, actor, TYPE, title, message, entityType, entityId, link, Map.of());
        Runnable run = () -> delivery.execute(() -> {
            try {
                publisher.publish(request);
            } catch (RuntimeException e) {
                log.warn("Shift notification failed: {}", title, e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    run.run();
                }
            });
        } else {
            run.run();
        }
    }
}
