package com.fashionsystem.fashion_system.payment;

/** Callback của cổng thanh toán sai chữ ký hoặc thiếu dữ liệu: không được tin và không xử lý. */
public class InvalidPaymentCallbackException extends RuntimeException {
    public InvalidPaymentCallbackException(String message) {
        super(message);
    }
}
