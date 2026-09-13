package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.PermissionCatalogAdministrationService;
import java.util.List;
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

/** Xác minh catalog Setting chỉ cho Admin có SETTINGS_MANAGE thao tác. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PermissionCatalogAdministrationControllerSecurityTest.TestConfiguration.class)
class PermissionCatalogAdministrationControllerSecurityTest {
    @Autowired
    private PermissionCatalogAdministrationController controller;

    @Autowired
    private PermissionCatalogAdministrationService service;

    @BeforeEach
    void resetMockInteractions() {
        clearInvocations(service);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerWithSettingsPermissionIsForbidden() {
        authenticate("ROLE_MANAGER", "SETTINGS_MANAGE");

        assertThrows(AccessDeniedException.class, controller::getModules);
        verifyNoInteractions(service);
    }

    @Test
    void adminWithoutSettingsPermissionIsForbidden() {
        authenticate("ROLE_ADMIN");

        assertThrows(AccessDeniedException.class, controller::getModules);
        verifyNoInteractions(service);
    }

    @Test
    void adminWithSettingsPermissionCanAccessCatalog() {
        authenticate("ROLE_ADMIN", "SETTINGS_MANAGE");

        assertDoesNotThrow(controller::getModules);
        verify(service).getModules();
    }

    private void authenticate(String... authorities) {
        var grantedAuthorities = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var principal = new AuthenticatedUser(UUID.randomUUID(), "admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean
        PermissionCatalogAdministrationService permissionCatalogAdministrationService() {
            return mock(PermissionCatalogAdministrationService.class);
        }

        @Bean
        PermissionCatalogAdministrationController permissionCatalogAdministrationController(
                PermissionCatalogAdministrationService service) {
            return new PermissionCatalogAdministrationController(service);
        }
    }
}
