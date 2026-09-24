package com.fashionsystem.fashion_system.audit;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class AuditEventSanitizer {
    private static final Set<String> BLOCKED = Set.of("password", "passwordhash", "token", "accesstoken", "refreshtoken", "authorization", "cookie", "requestbody", "body", "otp", "email", "phone");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private AuditEventSanitizer() {}
    public static Map<String, Object> sanitize(Map<String, ?> input) {
        if (input == null) return Map.of();
        return input.entrySet().stream()
                .filter(e -> e.getKey() != null && !BLOCKED.contains(normalize(e.getKey())))
                .limit(32)
                .collect(Collectors.toUnmodifiableMap(e -> e.getKey().substring(0, Math.min(64, e.getKey().length())), e -> sanitizeValue(e.getValue()), (a,b) -> a));
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

    private static Object sanitizeValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof JsonNode node) return sanitize(node);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safe = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                if (key == null) return;
                String field = key.toString();
                safe.put(field, BLOCKED.contains(normalize(field)) ? "[REDACTED]" : sanitizeValue(nested));
            });
            return Collections.unmodifiableMap(safe);
        }
        if (value instanceof Iterable<?> values) {
            List<Object> safe = new ArrayList<>();
            values.forEach(item -> safe.add(sanitizeValue(item)));
            return Collections.unmodifiableList(safe);
        }
        if (value.getClass().isArray()) {
            List<Object> safe = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) safe.add(sanitizeValue(Array.get(value, i)));
            return Collections.unmodifiableList(safe);
        }
        JsonNode tree = MAPPER.valueToTree(value);
        return sanitize(tree);
    }
}
