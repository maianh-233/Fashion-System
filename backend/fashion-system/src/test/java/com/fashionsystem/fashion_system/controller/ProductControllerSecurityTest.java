package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AuthorizationService;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductService;
import com.fashionsystem.fashion_system.service.UserScopeService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Covers Store read-only enforcement on Product itself, including direct-id mutations. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductControllerSecurityTest.TestConfiguration.class)
class ProductControllerSecurityTest {
    @Autowired ProductController controller;
    @Autowired ProductService productService;
    @Autowired AuthorizationService authorizationService;
    @Autowired UserScopeService userScopeService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        clearInvocations(productService, authorizationService, userScopeService);
        actorId = UUID.randomUUID();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storeUserCanReadProductListAndDetail() {
        authenticate("PRODUCT_VIEW");
        when(authorizationService.hasPermission(actorId, "PRODUCT_VIEW")).thenReturn(true);
        when(userScopeService.resolve(actorId)).thenReturn(new com.fashionsystem.fashion_system.service.UserScope(
                com.fashionsystem.fashion_system.service.UserScope.Kind.STORE, UUID.randomUUID(), "STORE", "Store"));
        UUID productId = UUID.randomUUID();
        when(productService.getById(productId)).thenReturn(ProductDto.builder().id(productId).status("ACTIVE").build());

        assertDoesNotThrow(() -> controller.getList(authentication(), null, null, null,
                null, null, null, null, PageRequest.of(0, 20)));
        assertDoesNotThrow(() -> controller.getById(authentication(), productId));

        verify(productService).getById(productId);
        verify(productService).getList(null, null, null, null, null, null, "ACTIVE", PageRequest.of(0, 20));
    }

    @Test
    void storeUserWithEveryProductMutationPermissionCannotMutate() {
        authenticate("PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE");
        when(authorizationService.hasPermission(actorId, "PRODUCT_CREATE")).thenReturn(true);
        when(authorizationService.hasPermission(actorId, "PRODUCT_UPDATE")).thenReturn(true);
        when(authorizationService.hasPermission(actorId, "PRODUCT_DELETE")).thenReturn(true);
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() -> controller.create(authentication(), validProduct()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> controller.update(authentication(), productId, validProduct()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> controller.delete(authentication(), productId))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(productService);
    }

    @Test
    void globalProductManagerCanUpdateWhenRbacAllows() {
        authenticate("PRODUCT_UPDATE");
        when(authorizationService.hasPermission(actorId, "PRODUCT_UPDATE")).thenReturn(true);
        UUID productId = UUID.randomUUID();

        assertDoesNotThrow(() -> controller.update(authentication(), productId, validProduct()));

        verify(userScopeService).requireGlobal(actorId);
        verify(productService).update(any(UUID.class), any(ProductDto.class));
    }

    private ProductDto validProduct() {
        return ProductDto.builder().name("Áo khoác").slug("ao-khoac").status("ACTIVE").build();
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
        @Bean ProductService productService() { return mock(ProductService.class); }
        @Bean AuthorizationService authorizationService() { return mock(AuthorizationService.class); }
        @Bean UserScopeService userScopeService() { return mock(UserScopeService.class); }
        @Bean ProductAuthorizationService productAuthorizationService(
                AuthorizationService authorizationService, UserScopeService userScopeService) {
            return new ProductAuthorizationService(authorizationService, userScopeService);
        }
        @Bean ProductController productController(
                ProductService service, ProductAuthorizationService authorization) {
            return new ProductController(service, authorization,
                    mock(com.fashionsystem.fashion_system.service.CatalogMediaService.class));
        }
    }
}
