package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.security.AuthenticatedCustomer;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AdminProfileService;
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

/** Xác minh endpoint hồ sơ admin không chấp nhận principal của customer. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AdminProfileControllerSecurityTest.TestConfiguration.class)
class AdminProfileControllerSecurityTest {
    @Autowired
    private AdminProfileController controller;

    @Autowired
    private AdminProfileService service;

    @BeforeEach
    void resetMockInteractions() {
        clearInvocations(service);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerPrincipalIsForbidden() {
        var customer = new AuthenticatedCustomer(UUID.randomUUID(), "customer");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                customer, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        assertThrows(AccessDeniedException.class, () -> controller.getProfile(null));
        verifyNoInteractions(service);
    }

    @Test
    void employeePrincipalCanReadOwnProfile() {
        var employee = new AuthenticatedUser(UUID.randomUUID(), "employee");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employee, null, List.of()));

        assertDoesNotThrow(() -> controller.getProfile(employee));
        verify(service).getProfile(employee.userId());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean
        AdminProfileService adminProfileService() {
            return mock(AdminProfileService.class);
        }

        @Bean
        AdminProfileController adminProfileController(AdminProfileService service) {
            return new AdminProfileController(service);
        }
    }
}
