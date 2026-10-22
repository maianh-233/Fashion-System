package com.fashionsystem.fashion_system.shipping;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình hãng vận chuyển (biến môi trường SHIPPING_* và LALAMOVE_*).
 *
 * @param clientId     Lalamove: API key
 * @param clientSecret Lalamove: API secret, dùng ký request và xác minh webhook
 * @param webhookUrl   URL công khai đã đăng ký với hãng; phần path tham gia chữ ký webhook
 */
@ConfigurationProperties(prefix = "integrations.shipping")
public record ShippingProperties(boolean enabled, String provider, String apiBaseUrl, String clientId,
                                 String clientSecret, String webhookUrl, Integer timeoutSeconds, Lalamove lalamove) {

    public record Lalamove(String market, String language, String serviceType) {
    }

    public int timeoutSecondsOrDefault() {
        return timeoutSeconds == null || timeoutSeconds <= 0 ? 15 : timeoutSeconds;
    }
}
