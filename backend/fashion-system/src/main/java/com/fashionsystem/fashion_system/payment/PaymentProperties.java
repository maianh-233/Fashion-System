package com.fashionsystem.fashion_system.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cổng thanh toán (biến môi trường PAYMENT_*, VNPAY_*, MOMO_*).
 *
 * @param holdMinutes     thời gian giữ hàng chờ khách thanh toán; quá hạn hệ thống tự hủy đơn
 * @param callbackBaseUrl URL công khai của backend, dùng dựng return URL và IPN URL gửi cho cổng
 * @param resultUrl       trang kết quả thanh toán phía khách (frontend) mà return URL chuyển tiếp tới
 */
@ConfigurationProperties(prefix = "integrations.payment")
public record PaymentProperties(Integer holdMinutes, String callbackBaseUrl, String resultUrl, Integer timeoutSeconds,
                                Vnpay vnpay, Momo momo) {

    /** VNPay 2.1.0: mã website (vnp_TmnCode) và chuỗi bí mật ký HMAC-SHA512 (vnp_HashSecret). */
    public record Vnpay(String tmnCode, String hashSecret, String payUrl) {
        public boolean configured() {
            return notBlank(tmnCode) && notBlank(hashSecret) && notBlank(payUrl);
        }
    }

    /** MoMo AIO v2 (captureWallet): partnerCode, accessKey, secretKey ký HMAC-SHA256. */
    public record Momo(String partnerCode, String accessKey, String secretKey, String apiBaseUrl) {
        public boolean configured() {
            return notBlank(partnerCode) && notBlank(accessKey) && notBlank(secretKey) && notBlank(apiBaseUrl);
        }
    }

    public int holdMinutesOrDefault() {
        return holdMinutes == null || holdMinutes <= 0 ? 15 : holdMinutes;
    }

    public int timeoutSecondsOrDefault() {
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 15 : timeoutSeconds;
    }

    /** URL công khai của backend cho một đường dẫn callback, ví dụ {@code /api/webhooks/payments/vnpay/ipn}. */
    public String callbackUrl(String path) {
        String base = callbackBaseUrl == null ? "" : callbackBaseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + path;
    }

    static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
