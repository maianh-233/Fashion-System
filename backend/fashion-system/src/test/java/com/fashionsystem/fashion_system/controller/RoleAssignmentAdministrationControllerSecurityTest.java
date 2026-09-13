package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.dto.RoleDto;
import com.fashionsystem.fashion_system.service.PermissionCatalogAdministrationService;
import com.fashionsystem.fashion_system.service.RoleAssignmentAdministrationService;
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
@ContextConfiguration(classes = RoleAssignmentAdministrationControllerSecurityTest.TestConfiguration.class)
class RoleAssignmentAdministrationControllerSecurityTest {
    @Autowired RoleAssignmentAdministrationController controller;
    @Autowired RoleAssignmentAdministrationService service;
    @Autowired PermissionCatalogAdministrationService catalogService;

    @BeforeEach
    void resetMocks() { clearInvocations(service, catalogService); }

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void roleViewCanReadRolesAndCatalog() {
        authenticate("ROLE_VIEW");

        assertDoesNotThrow(() -> controller.getRoles());
        assertDoesNotThrow(() -> controller.getRoleCatalog());

        verify(service).getRoles();
        verify(catalogService).getCatalogTree();
    }

    @Test
    void roleViewCannotCreateRole() {
        authenticate("ROLE_VIEW");

        assertThrows(AccessDeniedException.class,
                () -> controller.createRole(RoleDto.builder().code("TEST").name("Test").build()));
        verifyNoInteractions(service);
    }

    @Test
    void roleCreateCanCreateRole() {
        authenticate("ROLE_CREATE");
        RoleDto request = RoleDto.builder().code("TEST").name("Test").build();

        assertDoesNotThrow(() -> controller.createRole(request));

        verify(service).createRole(request);
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "tester", null, java.util.List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean RoleAssignmentAdministrationService roleService() {
            return mock(RoleAssignmentAdministrationService.class);
        }

        @Bean PermissionCatalogAdministrationService catalogService() {
            return mock(PermissionCatalogAdministrationService.class);
        }

        @Bean RoleAssignmentAdministrationController roleController(
                RoleAssignmentAdministrationService roleService,
                PermissionCatalogAdministrationService catalogService) {
            return new RoleAssignmentAdministrationController(roleService, catalogService);
        }
    }
}
