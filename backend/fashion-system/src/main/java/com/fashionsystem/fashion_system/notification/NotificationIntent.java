package com.fashionsystem.fashion_system.notification;

import java.util.UUID;

/** Local transactional signal emitted by a business workflow before commit. */
public record NotificationIntent(
        Workflow workflow,
        Action action,
        UUID entityId,
        String documentCode,
        UUID storeId,
        UUID actorUserId,
        UUID creatorUserId) {

    public enum Workflow {
        IMPORT_RECEIPT,
        EXPORT_RECEIPT
    }

    public enum Action {
        SUBMITTED,
        APPROVED
    }
}
