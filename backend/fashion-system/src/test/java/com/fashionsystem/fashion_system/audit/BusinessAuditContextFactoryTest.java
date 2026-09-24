package com.fashionsystem.fashion_system.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class BusinessAuditContextFactoryTest {
    private final BusinessAuditContextFactory factory = new BusinessAuditContextFactory();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void authenticatedRequestProvidesActorAndRequestMetadata() {
        UUID id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthenticatedUser(id, "alice"), null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/employees/1");
        request.addHeader("X-Request-ID", "req-1");
        request.addHeader("User-Agent", "browser");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        BusinessAuditContext context = factory.forCurrentRequest("EMPLOYEE", "changeUserAndRole");

        assertThat(context.actorType()).isEqualTo(AuditActorType.EMPLOYEE);
        assertThat(context.actorUserId()).isEqualTo(id);
        assertThat(context.username()).isEqualTo("alice");
        assertThat(context.action()).isEqualTo("EMPLOYEE_CHANGE_USER_AND_ROLE");
        assertThat(context.requestId()).isEqualTo("req-1");
        assertThat(context.method()).isEqualTo("PATCH");
        assertThat(context.path()).isEqualTo("/employees/1");
        assertThat(context.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(context.userAgent()).isEqualTo("browser");
    }

    @Test
    void explicitJobScopeProducesSystemActorAndJobName() {
        try (BusinessAuditContextFactory.JobScope ignored = BusinessAuditContextFactory.systemJob("nightly-rebuild")) {
            BusinessAuditContext context = factory.forCurrentRequest("INVENTORY", "rebuildStock");
            assertThat(context.actorType()).isEqualTo(AuditActorType.SYSTEM);
            assertThat(context.actorUserId()).isNull();
            assertThat(context.jobName()).isEqualTo("nightly-rebuild");
            assertThat(context.action()).isEqualTo("INVENTORY_REBUILD_STOCK");
        }
    }

    @Test
    void missingAuthenticatedActorRequiresExplicitJobScope() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> factory.forCurrentRequest("EMPLOYEE", "changeUserAndRole"))
                .isInstanceOf(IllegalStateException.class);
    }
}
