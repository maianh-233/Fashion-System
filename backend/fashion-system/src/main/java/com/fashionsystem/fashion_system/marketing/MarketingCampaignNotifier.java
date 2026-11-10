package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Thông báo nội bộ cho nhân viên (chờ duyệt, đã duyệt, bị từ chối), gửi sau commit trên luồng nền như các module khác.
 * Đây là thông báo vận hành cho nhân viên, không phải thông báo marketing gửi khách hàng.
 */
@Slf4j
@Component
class MarketingCampaignNotifier {
    static final String SUBMITTED = "MARKETING_CAMPAIGN_SUBMITTED";
    static final String APPROVED = "MARKETING_CAMPAIGN_APPROVED";
    static final String REJECTED = "MARKETING_CAMPAIGN_REJECTED";
    static final String DISPATCH_STALLED = "MARKETING_DISPATCH_STALLED";
    private static final int MAX_TITLE = 255;
    private static final int MAX_MESSAGE = 300;

    private final NotificationPublisher notifications;
    private final TaskExecutor executor;

    MarketingCampaignNotifier(NotificationPublisher notifications,
                              @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.notifications = notifications;
        this.executor = executor;
    }

    /** Người thực hiện thao tác không tự nhận thông báo của chính mình. */
    void send(Collection<UUID> recipients, UUID actor, String type, String title, String message, UUID campaignId, String code) {
        List<RealtimeIdentity> to = recipients.stream().filter(id -> id != null && !id.equals(actor)).distinct()
                .map(RealtimeIdentity::user).toList();
        if (to.isEmpty()) return;
        NotificationRequest request = new NotificationRequest(to, actor, type, cut(title, MAX_TITLE), cut(message, MAX_MESSAGE),
                "MARKETING_CAMPAIGN", campaignId, "/admin/marketing/campaigns/" + campaignId, Map.of("campaignCode", code));
        afterCommit(() -> {
            try {
                executor.execute(() -> deliver(request));
            } catch (RuntimeException rejected) {
                log.warn("Marketing campaign notification {} for {} was rejected: {}", type, code, rejected.getMessage());
            }
        });
    }

    private void deliver(NotificationRequest request) {
        try {
            notifications.publish(request);
        } catch (RuntimeException failure) {
            log.warn("Marketing campaign notification {} for {} failed: {}", request.type(), request.entityId(), failure.getMessage());
        }
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                action.run();
            }
        });
    }
}
