package com.fashionsystem.fashion_system.audit.spool;

import com.fashionsystem.fashion_system.audit.AuditEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public record AuditSpoolLine(UUID eventId, JsonNode payload, String payloadHash,
                             String previousHash, String lineHash) {
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final String GENESIS = "0".repeat(64);

    static AuditSpoolLine of(AuditEvent event, String previousHash) {
        JsonNode payload = MAPPER.valueToTree(event);
        String payloadHash = sha256(canonical(payload));
        return new AuditSpoolLine(event.eventId(), payload, payloadHash, previousHash,
                sha256(previousHash + payloadHash + event.eventId()));
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String canonical(JsonNode node) {
        if (node.isObject()) {
            var names = new java.util.ArrayList<>(node.propertyNames());
            names.sort(String::compareTo);
            var parts = new java.util.ArrayList<String>();
            for (String name : names) parts.add(MAPPER.writeValueAsString(name) + ":" + canonical(node.get(name)));
            return "{" + String.join(",", parts) + "}";
        }
        if (node.isArray()) {
            var parts = new java.util.ArrayList<String>();
            node.forEach(item -> parts.add(canonical(item)));
            return "[" + String.join(",", parts) + "]";
        }
        return MAPPER.writeValueAsString(node);
    }
}
