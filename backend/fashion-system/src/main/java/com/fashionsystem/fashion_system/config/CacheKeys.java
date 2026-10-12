package com.fashionsystem.fashion_system.config;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Builds deterministic, collision-resistant Spring Cache keys from supported immutable values. */
public final class CacheKeys {

    private CacheKeys() {
    }

    public static String of(Object... parts) {
        StringBuilder key = new StringBuilder();
        if (parts == null) {
            appendToken(key, "A", "null");
            return key.toString();
        }
        appendToken(key, "A", Integer.toString(parts.length));
        for (Object part : parts) append(key, part);
        return key.toString();
    }

    private static void append(StringBuilder key, Object value) {
        if (value == null) {
            key.append("N;");
        } else if (value instanceof String text) {
            appendToken(key, "S", text);
        } else if (value instanceof UUID uuid) {
            appendToken(key, "U", uuid.toString());
        } else if (value instanceof Boolean bool) {
            appendToken(key, "B", bool.toString());
        } else if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof BigInteger || value instanceof BigDecimal
                || value instanceof Float || value instanceof Double) {
            appendToken(key, "D" + value.getClass().getName(), value.toString());
        } else if (value instanceof Enum<?> enumValue) {
            appendToken(key, "E" + enumValue.getDeclaringClass().getName(), enumValue.name());
        } else if (value instanceof TemporalAccessor temporal) {
            appendToken(key, "T" + value.getClass().getName(), temporal.toString());
        } else if (value instanceof Pageable pageable) {
            appendPageable(key, pageable);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported cache key component: " + value.getClass().getName());
        }
    }

    private static void appendPageable(StringBuilder key, Pageable pageable) {
        key.append("P{");
        appendToken(key, "paged", Boolean.toString(pageable.isPaged()));
        if (pageable.isPaged()) {
            appendToken(key, "page", Integer.toString(pageable.getPageNumber()));
            appendToken(key, "size", Integer.toString(pageable.getPageSize()));
        }
        appendSort(key, pageable.getSort());
        key.append("};");
    }

    private static void appendSort(StringBuilder key, Sort sort) {
        key.append("O{");
        for (Sort.Order order : sort) {
            key.append("R{");
            appendToken(key, "property", order.getProperty());
            appendToken(key, "direction", order.getDirection().name());
            appendToken(key, "ignoreCase", Boolean.toString(order.isIgnoreCase()));
            appendToken(key, "nullHandling", order.getNullHandling().name());
            key.append("};");
        }
        key.append("};");
    }

    private static void appendToken(StringBuilder key, String type, String value) {
        key.append(type.length()).append(':').append(type)
                .append(value.length()).append(':').append(value).append(';');
    }
}
