package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditEventMetadataSanitizationTest {
    @Test
    void blocksCredentialHeadersAndCookies() {
        Map<String, Object> result = AuditEventSanitizer.sanitize(Map.of(
                "Authorization", "Bearer secret",
                "cookie", "session=secret",
                "accessToken", "secret",
                "refreshToken", "secret",
                "status", 200));

        assertThat(result).containsEntry("status", 200)
                .doesNotContainKeys("Authorization", "cookie", "accessToken", "refreshToken");
    }
}
