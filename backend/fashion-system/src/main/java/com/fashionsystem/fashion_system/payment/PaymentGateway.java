package com.fashionsystem.fashion_system.payment;

import com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput;
import com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentLink;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest;

/**
 * Cổng thanh toán online. Nghiệp vụ đơn hàng chỉ phụ thuộc interface này; thêm cổng mới chỉ cần thêm một
 * bản cài đặt và khai báo phương thức tương ứng.
 */
public interface PaymentGateway {

    /** Mã phương thức thanh toán trên đơn, ví dụ VNPAY, MOMO. */
    String code();

    /** Đã có đủ khóa trong cấu hình để dùng. */
    boolean configured();

    /** Tạo link trang thanh toán cho khách. */
    PaymentLink createPayment(PaymentRequest request);

    /**
     * Xác minh chữ ký và đọc kết quả từ IPN hoặc return URL.
     *
     * @throws InvalidPaymentCallbackException chữ ký sai hoặc thiếu dữ liệu bắt buộc
     */
    GatewayCallback parseCallback(CallbackInput input);
}
