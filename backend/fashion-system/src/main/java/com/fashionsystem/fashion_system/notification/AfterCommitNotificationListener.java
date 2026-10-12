package com.fashionsystem.fashion_system.notification;

import com.fashionsystem.fashion_system.entity.User;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Báo cho người tạo phiếu và người duyệt mỗi khi phiếu nhập/xuất đổi trạng thái.
 * Người duyệt là người đã xác nhận phiếu; trước khi có người xác nhận, đó là mọi người có quyền xác nhận
 * phiếu tại cửa hàng. Người thực hiện thao tác không bao giờ tự nhận thông báo của chính mình.
 */
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
            UUID actor = intent.actorUserId();
            Set<RealtimeIdentity> approvers = approvers(intent);
            approvers.remove(RealtimeIdentity.user(actor));
            boolean notifyCreator = intent.creatorUserId() != null && !intent.creatorUserId().equals(actor);
            if (notifyCreator) approvers.remove(RealtimeIdentity.user(intent.creatorUserId()));
            if (approvers.isEmpty() && !notifyCreator) return;

            String actorName = actorName(actor);
            if (!approvers.isEmpty()) {
                publish(intent, List.copyOf(approvers), approverMessage(intent, actorName));
            }
            if (notifyCreator) {
                publish(intent, List.of(RealtimeIdentity.user(intent.creatorUserId())), creatorMessage(intent, actorName));
            }
        } catch (RuntimeException failure) {
            log.warn(
                    "Post-commit notification failed for {} {}",
                    intent.workflow(),
                    intent.entityId(),
                    failure);
        }
    }

    private Set<RealtimeIdentity> approvers(NotificationIntent intent) {
        if (intent.approverUserId() != null) {
            return new LinkedHashSet<>(Set.of(RealtimeIdentity.user(intent.approverUserId())));
        }
        boolean awaitingApproval = intent.action() == NotificationIntent.Action.SUBMITTED
                || (intent.action() == NotificationIntent.Action.CANCELLED
                        && "PENDING_CONFIRMATION".equals(intent.previousStatus()));
        if (!awaitingApproval) return new LinkedHashSet<>();
        String permission = isImport(intent) ? "IMPORT_RECEIPT_CONFIRM" : "EXPORT_RECEIPT_CONFIRM";
        return new LinkedHashSet<>(recipientResolver.resolveUsers(permission, intent.storeId(), intent.actorUserId()));
    }

    private record Message(String type, String title, String body) {
    }

    private Message approverMessage(NotificationIntent intent, String actorName) {
        String kind = kind(intent);
        String code = intent.documentCode();
        return switch (intent.action()) {
            case SUBMITTED -> new Message(type(intent, "SUBMITTED"),
                    "Có phiếu " + kind + " cần duyệt",
                    actorName + " đã gửi phiếu " + code + " cho bạn duyệt.");
            case APPROVED -> new Message(type(intent, "APPROVED"),
                    "Phiếu " + kind + " đã được duyệt",
                    "Phiếu " + code + " đã được " + actorName + " duyệt.");
            case COMPLETED -> new Message(type(intent, "COMPLETED"),
                    "Phiếu " + kind + " đã hoàn tất",
                    actorName + " đã hoàn tất phiếu " + code + " bạn đã duyệt. Tồn kho đã được cập nhật.");
            case CANCELLED -> new Message(type(intent, "CANCELLED"),
                    "Phiếu " + kind + " đã bị hủy",
                    actorName + " đã hủy phiếu " + code + ", bạn không cần duyệt nữa.");
        };
    }

    private Message creatorMessage(NotificationIntent intent, String actorName) {
        String kind = kind(intent);
        String code = intent.documentCode();
        return switch (intent.action()) {
            case SUBMITTED -> new Message(type(intent, "SUBMITTED"),
                    "Phiếu " + kind + " đã được gửi duyệt",
                    actorName + " đã gửi phiếu " + code + " của bạn để duyệt.");
            case APPROVED -> new Message(type(intent, "APPROVED"),
                    "Phiếu " + kind + " đã được duyệt",
                    "Phiếu " + code + " của bạn đã được " + actorName + " duyệt.");
            case COMPLETED -> new Message(type(intent, "COMPLETED"),
                    "Phiếu " + kind + " đã hoàn tất",
                    actorName + " đã hoàn tất phiếu " + code + " của bạn. Tồn kho đã được cập nhật.");
            case CANCELLED -> new Message(type(intent, "CANCELLED"),
                    "Phiếu " + kind + " đã bị hủy",
                    actorName + " đã hủy phiếu " + code + " của bạn.");
        };
    }

    private void publish(NotificationIntent intent, List<RealtimeIdentity> recipients, Message message) {
        boolean receipt = isImport(intent);
        notificationPublisher.publish(new NotificationRequest(
                recipients,
                intent.actorUserId(),
                message.type(),
                message.title(),
                message.body(),
                receipt ? "PURCHASE_RECEIPT" : "STOCK_EXPORT",
                intent.entityId(),
                (receipt ? "/admin/imports" : "/admin/exports")
                        + "?storeId=" + intent.storeId() + "&receipt=" + intent.entityId(),
                metadata(intent)));
    }

    private static boolean isImport(NotificationIntent intent) {
        return intent.workflow() == NotificationIntent.Workflow.IMPORT_RECEIPT;
    }

    private static String kind(NotificationIntent intent) {
        return isImport(intent) ? "nhập" : "xuất";
    }

    /** PURCHASE_RECEIPT_* cho phiếu nhập, STOCK_EXPORT_* cho phiếu xuất (xem {@link NotificationType}). */
    private static String type(NotificationIntent intent, String action) {
        return (isImport(intent) ? "PURCHASE_RECEIPT_" : "STOCK_EXPORT_") + action;
    }

    private String actorName(UUID actorUserId) {
        if (actorUserId == null) return "Nhân viên";
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
