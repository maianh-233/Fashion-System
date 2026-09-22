package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

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
}
