package com.fashionsystem.fashion_system.shipping;

/** Lỗi khi gọi hãng vận chuyển (mạng, xác thực, dữ liệu bị từ chối). Message hiển thị được cho nhân viên. */
public class ShippingException extends RuntimeException {
    public ShippingException(String message) {
        super(message);
    }

    public ShippingException(String message, Throwable cause) {
        super(message, cause);
    }
}
