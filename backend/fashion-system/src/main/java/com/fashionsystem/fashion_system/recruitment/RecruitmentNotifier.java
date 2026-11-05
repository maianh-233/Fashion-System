package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
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

/** Thông báo trong app cho người được phân công (đánh giá, phỏng vấn, duyệt offer); gửi sau commit, trên luồng nền. */
@Slf4j
@Component
public class RecruitmentNotifier {
    public static final String TYPE = "RECRUITMENT_TASK";

    private final NotificationPublisher publisher;
    private final TaskExecutor delivery;

    public RecruitmentNotifier(NotificationPublisher publisher,
                               @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.publisher = publisher;
        this.delivery = delivery;
    }

    void notifyUsers(Collection<UUID> userIds, UUID actor, String title, String message, UUID applicationId) {
        List<RealtimeIdentity> recipients = userIds.stream().filter(Objects::nonNull).filter(id -> !id.equals(actor))
                .distinct().map(RealtimeIdentity::user).toList();
        if (recipients.isEmpty()) return;
        NotificationRequest request = new NotificationRequest(recipients, actor, TYPE, title, message, "JOB_APPLICATION",
                applicationId, "/admin/recruitment/applications/" + applicationId, Map.of());
        Runnable send = () -> delivery.execute(() -> {
            try {
                publisher.publish(request);
            } catch (RuntimeException e) {
                log.warn("Recruitment notification failed: {}", title, e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
