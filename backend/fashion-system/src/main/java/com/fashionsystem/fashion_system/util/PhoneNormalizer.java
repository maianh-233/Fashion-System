package com.fashionsystem.fashion_system.util;

import org.springframework.stereotype.Component;

/** Chuẩn hóa số điện thoại di động Việt Nam về E.164 để so khớp định danh ổn định. */
@Component
public class PhoneNormalizer {
    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại là bắt buộc");
        }
        String compact = value.trim().replace(" ", "").replace("-", "").replace(".", "");
        if (!compact.matches("\\+?[0-9]+")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        String digits = compact.startsWith("+") ? compact.substring(1) : compact;
        if (digits.matches("0[1-9][0-9]{8}")) {
            return "+84" + digits.substring(1);
        }
        if (digits.matches("84[1-9][0-9]{8}")) {
            return "+" + digits;
        }
        throw new IllegalArgumentException("Chỉ hỗ trợ số điện thoại Việt Nam hợp lệ");
    }

    public String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : normalize(value);
    }
}
