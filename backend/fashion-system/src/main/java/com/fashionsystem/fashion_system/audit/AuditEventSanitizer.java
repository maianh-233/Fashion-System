package com.fashionsystem.fashion_system.audit;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditEventSanitizer {
    private static final Set<String> BLOCKED = Set.of("password", "passwordhash", "token", "accesstoken", "refreshtoken", "authorization", "cookie", "requestbody", "body", "otp", "email", "phone");
    private AuditEventSanitizer() {}
    public static Map<String, Object> sanitize(Map<String, ?> input) {
        if (input == null) return Map.of();
        return input.entrySet().stream()
                .filter(e -> e.getKey() != null && !BLOCKED.contains(e.getKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "")))
                .limit(32)
                .collect(Collectors.toUnmodifiableMap(e -> e.getKey().substring(0, Math.min(64, e.getKey().length())), Map.Entry::getValue, (a,b) -> a));
    }
}
