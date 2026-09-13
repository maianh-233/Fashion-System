package com.fashionsystem.fashion_system.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.dto.GoodsIssueDto;
import com.fashionsystem.fashion_system.dto.GoodsReceiptDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.GoodsIssueService;
import com.fashionsystem.fashion_system.service.GoodsReceiptService;
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

/** Verifies receipt HTTP methods authorize and forward only the authenticated actor. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GoodsMovementControllerSecurityTest.TestConfiguration.class)
class GoodsMovementControllerSecurityTest {
    @Autowired GoodsReceiptController receiptController;
    @Autowired GoodsIssueController issueController;
    @Autowired GoodsReceiptService receiptService;
    @Autowired GoodsIssueService issueService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        clearInvocations(receiptService, issueService);
        actorId = UUID.randomUUID();
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void importCreateWithoutAuthorityIsDeniedBeforeService() {
        authenticate();
        assertThrows(AccessDeniedException.class,
                () -> receiptController.create(authentication(), new GoodsReceiptDto()));
        verifyNoInteractions(receiptService);
    }

    @Test
    void importCreateUsesPrincipalAsActor() {
        authenticate("IMPORT_RECEIPT_CREATE");
        GoodsReceiptDto request = new GoodsReceiptDto();
        request.setReceivedBy(UUID.randomUUID());
        assertDoesNotThrow(() -> receiptController.create(authentication(), request));
        verify(receiptService).create(actorId, request);
    }

    @Test
    void exportApproveUsesPrincipalAsActor() {
        authenticate("EXPORT_RECEIPT_APPROVE");
        UUID issueId = UUID.randomUUID();
        assertDoesNotThrow(() -> issueController.approve(authentication(), issueId));
        verify(issueService).approve(actorId, issueId);
    }

    @Test
    void exportCreateWithoutAuthorityIsDeniedBeforeService() {
        authenticate();
        assertThrows(AccessDeniedException.class,
                () -> issueController.create(authentication(), new GoodsIssueDto()));
        verifyNoInteractions(issueService);
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
        @Bean GoodsReceiptService goodsReceiptService() { return mock(GoodsReceiptService.class); }
        @Bean GoodsIssueService goodsIssueService() { return mock(GoodsIssueService.class); }
        @Bean GoodsReceiptController goodsReceiptController(GoodsReceiptService service) {
            return new GoodsReceiptController(service);
        }
        @Bean GoodsIssueController goodsIssueController(GoodsIssueService service) {
            return new GoodsIssueController(service);
        }
    }
}
