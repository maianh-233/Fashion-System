package com.fashionsystem.fashion_system.notification;

/** Common notification type constants. The persistence model intentionally accepts new values. */
public final class NotificationType {
    public static final String PURCHASE_RECEIPT_SUBMITTED = "PURCHASE_RECEIPT_SUBMITTED";
    public static final String PURCHASE_RECEIPT_APPROVED = "PURCHASE_RECEIPT_APPROVED";
    public static final String PURCHASE_RECEIPT_REJECTED = "PURCHASE_RECEIPT_REJECTED";
    public static final String PURCHASE_RECEIPT_COMPLETED = "PURCHASE_RECEIPT_COMPLETED";
    public static final String PURCHASE_RECEIPT_CANCELLED = "PURCHASE_RECEIPT_CANCELLED";
    public static final String STOCK_EXPORT_SUBMITTED = "STOCK_EXPORT_SUBMITTED";
    public static final String STOCK_EXPORT_APPROVED = "STOCK_EXPORT_APPROVED";
    public static final String STOCK_EXPORT_REJECTED = "STOCK_EXPORT_REJECTED";
    public static final String STOCK_EXPORT_COMPLETED = "STOCK_EXPORT_COMPLETED";
    public static final String STOCK_EXPORT_CANCELLED = "STOCK_EXPORT_CANCELLED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CUSTOMER_CONTACT = "ORDER_CUSTOMER_CONTACT";
    public static final String ORDER_PLACED = "ORDER_PLACED";
    public static final String ORDER_SPLIT = "ORDER_SPLIT";
    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String ORDER_CANCELLED_BY_CUSTOMER = "ORDER_CANCELLED_BY_CUSTOMER";
    public static final String ORDER_SHIPMENT_FAILED = "ORDER_SHIPMENT_FAILED";
    public static final String SUPPORT_CHAT_ESCALATED = "SUPPORT_CHAT_ESCALATED";
    public static final String SUPPORT_CHAT_MESSAGE = "SUPPORT_CHAT_MESSAGE";
    public static final String PROMOTION_ASSIGNED = "PROMOTION_ASSIGNED";
    public static final String PROMOTION_EXPIRING = "PROMOTION_EXPIRING";
    public static final String BACK_IN_STOCK = "BACK_IN_STOCK";
    public static final String RETURN_REQUESTED = "RETURN_REQUESTED";
    public static final String RETURN_STATUS_CHANGED = "RETURN_STATUS_CHANGED";
    public static final String STOCK_TRANSFER_REQUESTED = "STOCK_TRANSFER_REQUESTED";
    public static final String STOCK_TRANSFER_UPDATED = "STOCK_TRANSFER_UPDATED";
    public static final String SYSTEM = "SYSTEM";

    private NotificationType() {
    }
}
