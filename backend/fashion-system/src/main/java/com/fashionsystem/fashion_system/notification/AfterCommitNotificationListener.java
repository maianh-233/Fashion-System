package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterCommitNotificationListener {

    private final PermissionRecipientResolver recipientResolver;
    private final NotificationPublisher notificationPublisher;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationIntent intent) {
        try {
            if (intent.action() == NotificationIntent.Action.SUBMITTED) {
                publishSubmission(intent);
            } else if (intent.action() == NotificationIntent.Action.APPROVED) {
                publishApproval(intent);
            }
        } catch (RuntimeException failure) {
            log.warn(
                    "Post-commit notification failed for {} {}",
                    intent.workflow(),
                    intent.entityId(),
                    failure);
        }
    }

    private void publishSubmission(NotificationIntent intent) {
        String permission = intent.workflow() == NotificationIntent.Workflow.IMPORT_RECEIPT
                ? "IMPORT_RECEIPT_CONFIRM"
                : "EXPORT_RECEIPT_CONFIRM";
        Set<RealtimeIdentity> recipients = recipientResolver.resolveUsers(
                permission, intent.storeId(), intent.actorUserId());
        if (recipients.isEmpty()) return;

        boolean receipt = intent.workflow() == NotificationIntent.Workflow.IMPORT_RECEIPT;
        String actorName = receipt ? actorName(intent.actorUserId()) : null;
        notificationPublisher.publish(new NotificationRequest(
                List.copyOf(recipients),
                intent.actorUserId(),
                receipt ? NotificationType.PURCHASE_RECEIPT_SUBMITTED : NotificationType.STOCK_EXPORT_SUBMITTED,
                receipt ? "Có phiếu nhập cần duyệt" : "Có phiếu xuất cần duyệt",
                receipt
                        ? actorName + " đã gửi phiếu " + intent.documentCode() + " cho bạn duyệt."
                        : intent.documentCode() + " đang chờ bạn xác nhận.",
                receipt ? "PURCHASE_RECEIPT" : "STOCK_EXPORT",
                intent.entityId(),
                receipt ? "/admin/imports" : "/admin/exports",
                metadata(intent)));
    }

    private void publishApproval(NotificationIntent intent) {
        if (intent.creatorUserId() == null || intent.creatorUserId().equals(intent.actorUserId())) return;
        boolean receipt = intent.workflow() == NotificationIntent.Workflow.IMPORT_RECEIPT;
        notificationPublisher.publish(new NotificationRequest(
                List.of(RealtimeIdentity.user(intent.creatorUserId())),
                intent.actorUserId(),
                receipt ? NotificationType.PURCHASE_RECEIPT_APPROVED : NotificationType.STOCK_EXPORT_APPROVED,
                receipt ? "Phiếu nhập đã được duyệt" : "Phiếu xuất đã được duyệt",
                receipt
                        ? "Phiếu " + intent.documentCode() + " của bạn đã được "
                                + actorName(intent.actorUserId()) + " duyệt."
                        : intent.documentCode() + " đã được duyệt thành công.",
                receipt ? "PURCHASE_RECEIPT" : "STOCK_EXPORT",
                intent.entityId(),
                receipt ? "/admin/imports" : "/admin/exports",
                metadata(intent)));
    }

    private String actorName(java.util.UUID actorUserId) {
        return userRepository.findById(actorUserId)
                .map(this::displayName)
                .orElse("Nhân viên");
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
        return "Nhân viên";
    }

    private Map<String, Object> metadata(NotificationIntent intent) {
        return Map.of("storeId", intent.storeId().toString(), "documentCode", intent.documentCode());
    }
}
