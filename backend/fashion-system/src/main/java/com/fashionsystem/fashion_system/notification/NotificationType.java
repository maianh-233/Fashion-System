package com.fashionsystem.fashion_system.notification;

/** Common notification type constants. The persistence model intentionally accepts new values. */
public final class NotificationType {
    public static final String PURCHASE_RECEIPT_SUBMITTED = "PURCHASE_RECEIPT_SUBMITTED";
    public static final String PURCHASE_RECEIPT_APPROVED = "PURCHASE_RECEIPT_APPROVED";
    public static final String PURCHASE_RECEIPT_REJECTED = "PURCHASE_RECEIPT_REJECTED";
    public static final String STOCK_EXPORT_SUBMITTED = "STOCK_EXPORT_SUBMITTED";
    public static final String STOCK_EXPORT_APPROVED = "STOCK_EXPORT_APPROVED";
    public static final String STOCK_EXPORT_REJECTED = "STOCK_EXPORT_REJECTED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String PROMOTION_ASSIGNED = "PROMOTION_ASSIGNED";
    public static final String PROMOTION_EXPIRING = "PROMOTION_EXPIRING";
    public static final String SYSTEM = "SYSTEM";

    private NotificationType() {
    }
}
