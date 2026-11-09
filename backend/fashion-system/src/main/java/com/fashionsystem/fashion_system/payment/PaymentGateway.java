package com.fashionsystem.fashion_system.payment;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentLink;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Cổng thanh toán online. Nghiệp vụ đơn hàng chỉ phụ thuộc interface này; thêm cổng mới chỉ cần thêm một
 * bản cài đặt và khai báo phương thức tương ứng.
 */
public interface PaymentGateway {

    /** Mã phương thức thanh toán trên đơn, ví dụ VNPAY, MOMO. */
    String code();

    /** Đã có đủ khóa trong cấu hình để dùng. */
    boolean configured();

    /** Số tiền nhỏ nhất cổng chấp nhận (VND). */
    default long minAmount() {
        return 1;
    }

    /** Số tiền lớn nhất cổng chấp nhận (VND). */
    default long maxAmount() {
        return Long.MAX_VALUE;
    }

    /** Chặn trước khi giữ hàng: số tiền ngoài hạn mức thì khách không thể trả qua cổng này. */
    default void requireSupportedAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(minAmount())) >= 0 && amount.compareTo(BigDecimal.valueOf(maxAmount())) <= 0) return;
        throw BusinessException.badRequest("Số tiền " + String.format(Locale.GERMANY, "%,d", amount.longValue()) + "đ ngoài hạn mức của "
                + code() + ", vui lòng chọn thanh toán khi nhận hàng");
    }

    /** Tạo link trang thanh toán cho khách. */
    PaymentLink createPayment(PaymentRequest request);

    /**
     * Xác minh chữ ký và đọc kết quả từ IPN hoặc return URL.
     *
     * @throws InvalidPaymentCallbackException chữ ký sai hoặc thiếu dữ liệu bắt buộc
     */
    GatewayCallback parseCallback(CallbackInput input);
}
