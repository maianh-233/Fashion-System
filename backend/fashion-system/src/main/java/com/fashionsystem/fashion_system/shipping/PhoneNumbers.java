package com.fashionsystem.fashion_system.shipping;

/** Chuẩn hóa số điện thoại Việt Nam sang E.164 (+84...) theo yêu cầu của hãng vận chuyển. */
public final class PhoneNumbers {
    private PhoneNumbers() {
    }

    public static String toE164Vietnam(String phone) {
        if (phone == null || phone.isBlank()) throw new ShippingException("Thiếu số điện thoại liên hệ cho chuyến giao");
        String digits = phone.trim().replaceAll("[\\s.()-]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.startsWith("84")) return "+" + digits;
        if (digits.startsWith("0")) return "+84" + digits.substring(1);
        return "+84" + digits;
    }
}
