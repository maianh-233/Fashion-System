package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.StoreService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Bảo đảm thông tin cửa hàng không lộ cho quản lý thường dù họ có STORE_VIEW. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StoreControllerSecurityTest.TestConfiguration.class)
class StoreControllerSecurityTest {
    @Autowired StoreController controller;
    @Autowired StoreService service;

    @BeforeEach void reset() { clearInvocations(service); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void managerCannotSeeStoresEvenWithPermission() {
        authenticate("ROLE_MANAGER", "STORE_VIEW");
        assertThrows(AccessDeniedException.class,
                () -> controller.getList(null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(service);
    }

    @Test
    void adminNeedsStorePermission() {
        authenticate("ROLE_ADMIN");
        assertThrows(AccessDeniedException.class,
                () -> controller.getList(null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(service);
    }

    @Test
    void adminWithPermissionCanSeeStores() {
        authenticate("ROLE_ADMIN", "STORE_VIEW");
        assertDoesNotThrow(() -> controller.getList(null, null, PageRequest.of(0, 10)));
        verify(service).getList(isNull(), isNull(), any());
    }

    private void authenticate(String... authorities) {
        var granted = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        var principal = new AuthenticatedUser(UUID.randomUUID(), "employee");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, granted));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean StoreService storeService() { return mock(StoreService.class); }
        @Bean StoreController storeController(StoreService service) { return new StoreController(service); }
    }
}
