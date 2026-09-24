package com.fashionsystem.fashion_system.audit;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

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

    /** Returns a redacted copy; the source snapshot is never modified. */
    public static JsonNode sanitize(JsonNode input) {
        if (input == null) return null;
        if (input.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            input.properties().forEach(entry -> result.set(entry.getKey(),
                    BLOCKED.contains(normalize(entry.getKey()))
                            ? JsonNodeFactory.instance.stringNode("[REDACTED]") : sanitize(entry.getValue())));
            return result;
        }
        if (input.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            input.forEach(item -> result.add(sanitize(item)));
            return result;
        }
        return input.deepCopy();
    }

    private static String normalize(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }
}
