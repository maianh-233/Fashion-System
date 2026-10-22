package com.fashionsystem.fashion_system.payment;

/** Gọi cổng thanh toán thất bại (mạng, cổng từ chối yêu cầu tạo link...). */
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
