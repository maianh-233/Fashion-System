package com.fashionsystem.fashion_system.notification;

import java.util.UUID;

/**
 * Local transactional signal emitted by a business workflow before commit.
 *
 * @param approverUserId người đã xác nhận phiếu, null khi phiếu chưa được xác nhận
 * @param previousStatus trạng thái trước khi chuyển, dùng để biết người duyệt đã được báo hay chưa
 */
public record NotificationIntent(
        Workflow workflow,
        Action action,
        UUID entityId,
        String documentCode,
        UUID storeId,
        UUID actorUserId,
        UUID creatorUserId,
        UUID approverUserId,
        String previousStatus) {

    public enum Workflow {
        IMPORT_RECEIPT,
        EXPORT_RECEIPT
    }

    public enum Action {
        SUBMITTED,
        APPROVED,
        COMPLETED,
        CANCELLED
    }
}
