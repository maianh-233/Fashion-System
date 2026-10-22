package com.fashionsystem.fashion_system.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** Dữ liệu trao đổi giữa nghiệp vụ thanh toán và một cổng cụ thể. */
public final class PaymentModels {
    private PaymentModels() {
    }

    /**
     * Yêu cầu tạo link thanh toán.
     *
     * @param reference mã tham chiếu duy nhất của lần thanh toán (vnp_TxnRef / orderId của MoMo)
     * @param amount    số tiền VND, số nguyên
     * @param createdAt giờ Việt Nam (GMT+7)
     * @param expiresAt giờ Việt Nam (GMT+7)
     */
    public record PaymentRequest(String reference, long amount, String orderInfo, String clientIp,
                                 LocalDateTime createdAt, LocalDateTime expiresAt, String returnUrl, String ipnUrl) {
    }

    /** Link thanh toán do cổng trả về; {@code rawResponse} là JSON gốc (null nếu link được dựng tại chỗ). */
    public record PaymentLink(String payUrl, String rawResponse) {
    }

    /** Callback từ cổng: tham số query (VNPay) hoặc body JSON (MoMo). */
    public record CallbackInput(Map<String, String> params, String body) {
    }

    /** Kết quả cuối cùng của một lần thanh toán theo cổng. */
    public enum Outcome {
        SUCCESS, FAILED, PENDING
    }

    /**
     * Callback đã xác minh chữ ký.
     *
     * @param reference            mã tham chiếu đã gửi cổng lúc tạo link
     * @param gatewayTransactionId mã giao dịch phía cổng (có thể null khi khách hủy trước khi trả)
     * @param amount               số tiền VND cổng báo
     * @param code                 mã kết quả gốc của cổng
     * @param eventKey             định danh sự kiện để chống xử lý trùng
     * @param rawJson              dữ liệu gốc dạng JSON để lưu vết
     */
    public record GatewayCallback(String reference, String gatewayTransactionId, BigDecimal amount, Outcome outcome,
                                  String code, String message, String eventKey, String rawJson) {
    }
}
