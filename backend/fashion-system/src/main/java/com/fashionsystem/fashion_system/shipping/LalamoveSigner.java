package com.fashionsystem.fashion_system.shipping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chữ ký HMAC-SHA256 của Lalamove API v3 (developers.lalamove.com, mục Authentication):
 * {@code SIGNATURE = HmacSHA256ToHex("<TIMESTAMP>\r\n<HTTP_VERB>\r\n<PATH>\r\n\r\n<BODY>", <SECRET>)},
 * header {@code Authorization: hmac <KEY>:<TIMESTAMP>:<SIGNATURE>}. Webhook dùng cùng công thức với verb POST,
 * path của URL webhook và body là {@code JSON.stringify(data)}.
 */
public final class LalamoveSigner {
    private LalamoveSigner() {
    }

    public static String sign(String timestamp, String method, String path, String body, String secret) {
        String raw = timestamp + "\r\n" + method + "\r\n" + path + "\r\n\r\n" + (body == null ? "" : body);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("HmacSHA256 unavailable", failure);
        }
    }

    public static String authorization(String apiKey, String timestamp, String signature) {
        return "hmac " + apiKey + ":" + timestamp + ":" + signature;
    }

    /** So sánh hằng thời gian để không lộ chữ ký qua thời gian phản hồi. */
    public static boolean matches(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null) return false;
        return MessageDigest.isEqual(expectedHex.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actualHex.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
