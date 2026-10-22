package com.fashionsystem.fashion_system.service;

import java.util.Set;

/** Trạng thái thanh toán trên đơn ({@code orders.payment_status}) và trên từng lần thanh toán ({@code payments.status}). */
public final class OrderPaymentStatus {
    private OrderPaymentStatus() {
    }

    // Trên đơn.
    public static final String UNPAID = "UNPAID";
    /** Đơn trả qua cổng đang chờ khách thanh toán: cửa hàng chưa thấy đơn. */
    public static final String AWAITING_PAYMENT = "AWAITING_PAYMENT";
    /** Đơn trả qua cổng bị hủy khi chưa thanh toán (quá hạn hoặc khách tự hủy): cửa hàng không thấy đơn. */
    public static final String ABANDONED = "ABANDONED";
    public static final String PAID = "PAID";
    public static final String PARTIALLY_PAID = "PARTIALLY_PAID";

    /** Đơn có các trạng thái này không hiện cho cửa hàng (danh sách, thống kê, thông báo). */
    public static final Set<String> HIDDEN_FROM_STORE = Set.of(AWAITING_PAYMENT, ABANDONED);

    // Trên một lần thanh toán.
    public static final String ATTEMPT_PENDING = "PENDING";
    public static final String ATTEMPT_SUCCESS = "SUCCESS";
    public static final String ATTEMPT_FAILED = "FAILED";
    /** Link không còn dùng: khách đổi sang COD, đơn đã trả bằng lần khác, hoặc đơn đã hủy. */
    public static final String ATTEMPT_CANCELLED = "CANCELLED";

    public static boolean hiddenFromStore(String paymentStatus) {
        return paymentStatus != null && HIDDEN_FROM_STORE.contains(paymentStatus);
    }
}
