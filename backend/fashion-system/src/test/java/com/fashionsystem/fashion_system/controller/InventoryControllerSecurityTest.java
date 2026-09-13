package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.dto.InventoryAdjustmentRequest;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.InventoryService;
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

/** Verifies Inventory endpoints pass only the authenticated actor into service authorization. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = InventoryControllerSecurityTest.TestConfiguration.class)
class InventoryControllerSecurityTest {
    @Autowired InventoryController controller;
    @Autowired InventoryService service;
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
    void missingInventoryViewAuthorityIsDeniedBeforeService() {
        authenticate();

        assertThrows(AccessDeniedException.class, () -> controller.getBalances(
                authentication(), null, null, null, PageRequest.of(0, 20)));
        verifyNoInteractions(service);
    }

    @Test
    void listPassesAuthenticatedActorAndUntrustedStoreFilterToService() {
        authenticate("INVENTORY_VIEW");
        UUID requestedStore = UUID.randomUUID();

        assertDoesNotThrow(() -> controller.getBalances(authentication(), requestedStore,
                null, null, PageRequest.of(0, 20)));

        verify(service).getBalances(eq(actorId), eq(requestedStore), eq(null), eq(null),
                eq(PageRequest.of(0, 20)));
    }

    @Test
    void adjustmentIgnoresAnyClientActorAndUsesPrincipal() {
        authenticate("INVENTORY_ADJUST");
        UUID storeId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(storeId, variantId, 2);

        assertDoesNotThrow(() -> controller.adjust(authentication(), request));

        verify(service).adjust(actorId, storeId, variantId, 2);
    }

    private void authenticate(String... authorities) {
        var granted = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(actorId, "employee"), null, granted));
    }

    private org.springframework.security.core.Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean InventoryService inventoryService() { return mock(InventoryService.class); }
        @Bean InventoryController inventoryController(InventoryService service) {
            return new InventoryController(service);
        }
    }
}
