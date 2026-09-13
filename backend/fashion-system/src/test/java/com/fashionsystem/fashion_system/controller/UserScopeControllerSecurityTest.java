package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.UserScope;
import com.fashionsystem.fashion_system.service.UserScopeService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Ensures the scope endpoint only resolves the authenticated employee principal. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserScopeControllerSecurityTest.TestConfiguration.class)
class UserScopeControllerSecurityTest {
    @Autowired UserScopeController controller;
    @Autowired UserScopeService service;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        clearInvocations(service);
        actorId = UUID.randomUUID();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void endpointUsesOnlyAuthenticatedActorId() {
        authenticate();
        when(service.resolve(actorId)).thenReturn(UserScope.global());

        var result = assertDoesNotThrow(() -> controller.getCurrentScope(authentication()));

        assertThat(result.scope()).isEqualTo("GLOBAL");
        verify(service).resolve(actorId);
    }

    @Test
    void nonEmployeePrincipalIsForbidden() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer", null, java.util.List.of()));

        assertThrows(AccessDeniedException.class, () -> controller.getCurrentScope(authentication()));
    }

    private void authenticate() {
        var principal = new AuthenticatedUser(actorId, "employee");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }

    private org.springframework.security.core.Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean UserScopeService userScopeService() { return mock(UserScopeService.class); }
        @Bean UserScopeController userScopeController(UserScopeService service) {
            return new UserScopeController(service);
        }
    }
}
