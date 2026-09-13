package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AuthorizationService;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductImageService;
import com.fashionsystem.fashion_system.service.UserScopeService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Ensures Product image writes remain Global-only even with Variant update permission. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductImageControllerSecurityTest.TestConfiguration.class)
class ProductImageControllerSecurityTest {
    @Autowired ProductImageController controller;
    @Autowired ProductImageService imageService;
    @Autowired AuthorizationService authorizationService;
    @Autowired UserScopeService userScopeService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        clearInvocations(imageService, authorizationService, userScopeService);
        actorId = UUID.randomUUID();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storeUserCannotUploadImageWithVariantUpdatePermission() {
        authenticate("PRODUCT_VARIANT_UPDATE");
        when(authorizationService.hasPermission(actorId, "PRODUCT_VARIANT_UPDATE")).thenReturn(true);
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);

        assertThatThrownBy(() -> controller.upload(authentication(), UUID.randomUUID(), UUID.randomUUID(),
                new MockMultipartFile("file", "image.png", "image/png", new byte[] {1}), false, 0))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(imageService);
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
        @Bean ProductImageService productImageService() { return mock(ProductImageService.class); }
        @Bean AuthorizationService authorizationService() { return mock(AuthorizationService.class); }
        @Bean UserScopeService userScopeService() { return mock(UserScopeService.class); }
        @Bean ProductAuthorizationService productAuthorizationService(
                AuthorizationService authorizationService, UserScopeService userScopeService) {
            return new ProductAuthorizationService(authorizationService, userScopeService);
        }
        @Bean ProductImageController productImageController(
                ProductImageService service, ProductAuthorizationService authorization) {
            return new ProductImageController(service, authorization);
        }
    }
}
