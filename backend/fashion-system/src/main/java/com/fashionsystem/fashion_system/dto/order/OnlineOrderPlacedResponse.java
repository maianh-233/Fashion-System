package com.fashionsystem.fashion_system.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Đơn online vừa đặt; {@code parts} có nhiều phần khi đơn được tách cho nhiều cửa hàng.
 *
 * @param paymentDueAt hạn thanh toán khi khách chọn VNPay/MoMo (null với COD)
 * @param payment      link thanh toán vừa tạo (null với COD hoặc khi tạo link lỗi)
 * @param paymentError lý do chưa tạo được link; khách thử lại hoặc đổi sang COD
 */
public record OnlineOrderPlacedResponse(UUID id, String orderCode, BigDecimal total, boolean split,
                                        List<OnlineQuoteResponse.Part> parts, String message, String paymentMethod,
                                        String paymentStatus, LocalDateTime paymentDueAt, PaymentStartResponse payment,
                                        String paymentError) {

    public OnlineOrderPlacedResponse withPayment(PaymentStartResponse payment, String paymentError) {
        return new OnlineOrderPlacedResponse(id, orderCode, total, split, parts, message, paymentMethod, paymentStatus,
                paymentDueAt, payment, paymentError);
    }
}
