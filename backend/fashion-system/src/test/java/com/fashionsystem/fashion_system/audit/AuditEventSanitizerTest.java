package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class AuditEventSanitizerTest {
    @Test
    void removesSecretsAndSensitivePayloads() {
        Map<String, Object> safe = AuditEventSanitizer.sanitize(Map.of(
                "status", 401,
                "password", "secret",
                "accessToken", "token",
                "requestBody", "body",
                "email", "person@example.com"));

        assertThat(safe).containsEntry("status", 401);
        assertThat(safe).doesNotContainKeys("password", "accessToken", "requestBody", "email");
    }

    @Test
    void redactsSecretsRecursivelyInObjectsAndArrays() {
        JsonNode source = new ObjectMapper().readTree("{\"profile\":{\"passwordHash\":\"x\",\"name\":\"An\"},\"sessions\":[{\"token\":\"y\"}],\"email\":\"a@b.com\"}");
        JsonNode result = AuditEventSanitizer.sanitize(source);

        assertThat(result.at("/profile/passwordHash").asText()).isEqualTo("[REDACTED]");
        assertThat(result.at("/sessions/0/token").asText()).isEqualTo("[REDACTED]");
        assertThat(result.get("email").asText()).isEqualTo("[REDACTED]");
        assertThat(result.at("/profile/name").asText()).isEqualTo("An");
        assertThat(source.at("/profile/passwordHash").asText()).isEqualTo("x");
    }
}
