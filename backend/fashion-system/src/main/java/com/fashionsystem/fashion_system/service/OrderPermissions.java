package com.fashionsystem.fashion_system.service;

/** Mã quyền của module đơn hàng (bảng permissions, nhóm ORDER). */
public final class OrderPermissions {
    public static final String VIEW = "ORDER_VIEW";
    /** Tạo đơn bán tại quầy (offline). */
    public static final String CREATE_OFFLINE = "ORDER_CREATE";
    public static final String CONFIRM = "ORDER_CONFIRM";
    public static final String CANCEL = "ORDER_CANCEL";
    public static final String HANDOVER_SHIPPING = "ORDER_HANDOVER_SHIPPING";
    public static final String HIDE = "ORDER_HIDE";
    public static final String RESTORE = "ORDER_RESTORE";
    public static final String CONTACT_CUSTOMER = "ORDER_CONTACT_CUSTOMER";
    public static final String SUPPORT_CHAT = "ORDER_SUPPORT_CHAT";
    /** Sửa câu trả lời chính sách của bot. */
    public static final String SUPPORT_FAQ_MANAGE = "SUPPORT_FAQ_MANAGE";
    /** Xác nhận nhận hàng hoàn cho đơn do nhân viên khác xử lý. */
    public static final String RETURN_OVERRIDE = "ORDER_RETURN_OVERRIDE";

    private OrderPermissions() {
    }
}
