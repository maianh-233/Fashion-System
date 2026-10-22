package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.notification.PermissionRecipientResolver;
import com.fashionsystem.fashion_system.realtime.RealtimeEvent;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.realtime.RealtimePublisher;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Đẩy tin hội thoại realtime (kênh SUPPORT_CHAT) tới khách và nhân viên liên quan, và tạo thông báo cho nhân viên
 * khi bot chuyển hội thoại hoặc khách nhắn tin mới. Chạy sau commit trên luồng nền.
 */
@Slf4j
@Component
public class SupportChatNotificationListener {
    static final String CHANNEL = "SUPPORT_CHAT";

    private final RealtimePublisher realtime;
    private final NotificationPublisher notifications;
    private final PermissionRecipientResolver recipients;
    private final TaskExecutor executor;

    public SupportChatNotificationListener(RealtimePublisher realtime, NotificationPublisher notifications,
                                           PermissionRecipientResolver recipients,
                                           @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.realtime = realtime;
        this.notifications = notifications;
        this.recipients = recipients;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessage(SupportChatEvents.MessageCreated event) {
        executor.execute(() -> {
            try {
                deliver(event);
            } catch (RuntimeException failure) {
                log.warn("Support chat delivery failed for room {}", event.roomId(), failure);
            }
        });
    }

    void deliver(SupportChatEvents.MessageCreated event) {
        Set<RealtimeIdentity> staff = staffAudience(event);
        Map<String, Object> payload = Map.of("roomId", event.roomId(), "message", event.message(), "room", event.room());
        realtime.sendToIdentity(RealtimeIdentity.customer(event.customerId()),
                RealtimeEvent.create("MESSAGE_CREATED", CHANNEL, RealtimeIdentity.customer(event.customerId()), payload));
        staff.forEach(identity -> realtime.sendToIdentity(identity,
                RealtimeEvent.create("MESSAGE_CREATED", CHANNEL, identity, payload)));

        String link = "/admin/support?room=" + event.roomId();
        if (event.escalated())
            notify(staff, NotificationType.SUPPORT_CHAT_ESCALATED, "Khách cần hỗ trợ đơn " + event.orderCode(),
                    "Bot đã chuyển cuộc trò chuyện cho nhân viên. Vui lòng nhận hỗ trợ khách.", event, link);
        else if (event.firstUnread())
            notify(staff, NotificationType.SUPPORT_CHAT_MESSAGE, "Tin nhắn mới về đơn " + event.orderCode(),
                    event.message().content(), event, link);
    }

    /** Nhân viên đang nhận hội thoại; chưa ai nhận thì nhân viên hỗ trợ của các cửa hàng giao đơn. */
    private Set<RealtimeIdentity> staffAudience(SupportChatEvents.MessageCreated event) {
        if (event.assignedStaffId() != null) return Set.of(RealtimeIdentity.user(event.assignedStaffId()));
        Set<RealtimeIdentity> audience = new LinkedHashSet<>();
        for (var store : event.storeIds())
            audience.addAll(recipients.resolveStoreStaff(OrderPermissions.SUPPORT_CHAT, store));
        return audience;
    }

    private void notify(Set<RealtimeIdentity> to, String type, String title, String body,
                        SupportChatEvents.MessageCreated event, String link) {
        if (to.isEmpty()) return;
        String text = body == null || body.isBlank() ? title : body.length() > 200 ? body.substring(0, 200) + "…" : body;
        notifications.publish(new NotificationRequest(List.copyOf(to), null, type, title, text, "SUPPORT_CHAT",
                event.roomId(), link, Map.of("orderCode", event.orderCode())));
    }
}
