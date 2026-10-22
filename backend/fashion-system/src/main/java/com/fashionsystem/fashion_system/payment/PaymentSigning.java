package com.fashionsystem.fashion_system.payment;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Chữ ký HMAC dạng hex dùng chung cho các cổng thanh toán. */
final class PaymentSigning {
    private PaymentSigning() {
    }

    static String hmacSha512Hex(String secret, String data) {
        return hmacHex("HmacSHA512", secret, data);
    }

    static String hmacSha256Hex(String secret, String data) {
        return hmacHex("HmacSHA256", secret, data);
    }

    /** So sánh chữ ký hex không phân biệt hoa thường, thời gian không phụ thuộc vị trí khác nhau. */
    static boolean constantTimeEqualsIgnoreCase(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private static String hmacHex(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Không ký được dữ liệu thanh toán", e);
        }
    }
}
