package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AuthorizationService;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductVariantService;
import com.fashionsystem.fashion_system.service.UserScopeService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Covers Variant read access and Store mutation denial, including SKU/status changes. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductVariantControllerSecurityTest.TestConfiguration.class)
class ProductVariantControllerSecurityTest {
    @Autowired ProductVariantController controller;
    @Autowired ProductVariantService variantService;
    @Autowired AuthorizationService authorizationService;
    @Autowired UserScopeService userScopeService;
    private UUID actorId;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        clearInvocations(variantService, authorizationService, userScopeService);
        actorId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storeUserMayViewVariant() {
        authenticate("PRODUCT_VARIANT_VIEW");
        when(authorizationService.hasPermission(actorId, "PRODUCT_VARIANT_VIEW")).thenReturn(true);
        when(variantService.getById(productId, variantId)).thenReturn(ProductVariantDto.builder()
                .id(variantId).productId(productId).active(true).build());

        assertDoesNotThrow(() -> controller.getById(authentication(), productId, variantId));

        verify(variantService).getById(productId, variantId);
    }

    @Test
    void storeUserWithVariantUpdatePermissionCannotEditOrDeactivate() {
        authenticate("PRODUCT_VARIANT_UPDATE");
        when(authorizationService.hasPermission(actorId, "PRODUCT_VARIANT_UPDATE")).thenReturn(true);
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);

        assertThatThrownBy(() -> controller.update(authentication(), productId, variantId, validVariant()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> controller.deactivate(authentication(), productId, variantId))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(variantService);
    }

    private ProductVariantDto validVariant() {
        return ProductVariantDto.builder().sku("SKU-BLACK-M").price(BigDecimal.valueOf(250_000)).build();
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
        @Bean ProductVariantService productVariantService() { return mock(ProductVariantService.class); }
        @Bean AuthorizationService authorizationService() { return mock(AuthorizationService.class); }
        @Bean UserScopeService userScopeService() { return mock(UserScopeService.class); }
        @Bean ProductAuthorizationService productAuthorizationService(
                AuthorizationService authorizationService, UserScopeService userScopeService) {
            return new ProductAuthorizationService(authorizationService, userScopeService);
        }
        @Bean ProductVariantController productVariantController(
                ProductVariantService service, ProductAuthorizationService authorization) {
            return new ProductVariantController(service, authorization);
        }
    }
}
