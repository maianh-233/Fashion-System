package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.config.RefreshTokenCookieService;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AuthService;
import com.fashionsystem.fashion_system.service.AuthorizationService;
import com.fashionsystem.fashion_system.service.RefreshTokenService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AuthControllerEmployeeScopeSecurityTest.TestConfiguration.class)
class AuthControllerEmployeeScopeSecurityTest {
    @Autowired AuthController controller;
    @Autowired AuthService authService;
    @Autowired AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        clearInvocations(authService, authorizationService);
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "employee"), null,
                java.util.List.of(new SimpleGrantedAuthority("USER_CREATE")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void legacyEmployeeRegistrationRejectsStoreScopedPermission() {
        when(authorizationService.hasPermission(any(org.springframework.security.core.Authentication.class), org.mockito.ArgumentMatchers.eq("USER_CREATE"),
                org.mockito.ArgumentMatchers.eq(PermissionScope.ALL))).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.registerEmployee(null));

        verifyNoInteractions(authService);
    }

    @Test
    void legacyEmployeeRegistrationRemainsAvailableToAllScopePermission() {
        when(authorizationService.hasPermission(any(org.springframework.security.core.Authentication.class), org.mockito.ArgumentMatchers.eq("USER_CREATE"),
                org.mockito.ArgumentMatchers.eq(PermissionScope.ALL))).thenReturn(true);

        assertDoesNotThrow(() -> controller.registerEmployee(null));

        verify(authService).registerEmployee(null);
    }

    @Test
    void adminRegistrationAlsoRequiresAllDataScope() {
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(UUID.randomUUID(), "employee"), null,
                java.util.List.of(new SimpleGrantedAuthority("USER_CREATE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authorizationService.hasPermission(any(org.springframework.security.core.Authentication.class),
                org.mockito.ArgumentMatchers.eq("USER_CREATE_ADMIN"),
                org.mockito.ArgumentMatchers.eq(PermissionScope.ALL))).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> controller.registerAdmin(null));

        verifyNoInteractions(authService);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean AuthService authService() { return mock(AuthService.class); }
        @Bean RefreshTokenService refreshTokenService() { return mock(RefreshTokenService.class); }
        @Bean RefreshTokenCookieService refreshTokenCookieService() { return mock(RefreshTokenCookieService.class); }
        @Bean AuthorizationService authorizationService() { return mock(AuthorizationService.class); }
        @Bean AuthController authController(AuthService authService, RefreshTokenService refreshTokenService,
                RefreshTokenCookieService refreshTokenCookieService) {
            return new AuthController(authService, refreshTokenService, refreshTokenCookieService);
        }
    }
}
