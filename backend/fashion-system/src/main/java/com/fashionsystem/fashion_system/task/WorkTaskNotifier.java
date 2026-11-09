package com.fashionsystem.fashion_system.task;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Task;
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

/** Gửi thông báo công việc sau commit, trên luồng nền để Kafka chậm không giữ request của người dùng. */
@Slf4j
@Component
class WorkTaskNotifier {
    private static final int MAX_TITLE = 255;
    private static final int MAX_MESSAGE = 300;

    private final NotificationPublisher notifications;
    private final TaskExecutor executor;

    WorkTaskNotifier(NotificationPublisher notifications,
                     @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.notifications = notifications;
        this.executor = executor;
    }

    /** Người thực hiện thao tác không tự nhận thông báo của chính mình. */
    void send(Collection<UUID> recipients, UUID actor, String type, String title, String message, Task task) {
        List<RealtimeIdentity> to = recipients.stream().filter(id -> id != null && !id.equals(actor)).map(RealtimeIdentity::user).toList();
        if (to.isEmpty()) return;
        NotificationRequest request = new NotificationRequest(to, actor, type, cut(title, MAX_TITLE), cut(message, MAX_MESSAGE),
                "WORK_TASK", task.id(), "/admin/me/tasks/" + task.id(), Map.of("taskCode", task.code()));
        afterCommit(() -> {
            try {
                executor.execute(() -> deliver(request));
            } catch (RuntimeException rejected) {
                log.warn("Work task notification {} for {} was rejected: {}", type, task.code(), rejected.getMessage());
            }
        });
    }

    private void deliver(NotificationRequest request) {
        try {
            notifications.publish(request);
        } catch (RuntimeException failure) {
            log.warn("Work task notification {} for {} failed: {}", request.type(), request.entityId(), failure.getMessage());
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
