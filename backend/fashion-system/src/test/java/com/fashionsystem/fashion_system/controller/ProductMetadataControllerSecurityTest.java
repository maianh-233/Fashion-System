package com.fashionsystem.fashion_system.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.ProductAttributeDto;
import com.fashionsystem.fashion_system.dto.ProductTagDto;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AuthorizationService;
import com.fashionsystem.fashion_system.service.ProductAttributeService;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductTagMappingService;
import com.fashionsystem.fashion_system.service.ProductTagService;
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

/** Covers Store read-only enforcement for Product tags, mappings, and attributes. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductMetadataControllerSecurityTest.TestConfiguration.class)
class ProductMetadataControllerSecurityTest {
    @Autowired ProductTagController tagController;
    @Autowired ProductAttributeController attributeController;
    @Autowired ProductTagService tagService;
    @Autowired ProductAttributeService attributeService;
    @Autowired ProductTagMappingService mappingService;
    @Autowired AuthorizationService authorizationService;
    @Autowired UserScopeService userScopeService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        clearInvocations(tagService, attributeService, mappingService,
                authorizationService, userScopeService);
        actorId = UUID.randomUUID();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storeUserMayReadTagsAndAttributes() {
        authenticate("TAG_VIEW", "PRODUCT_VIEW");
        when(authorizationService.hasPermission(actorId, "TAG_VIEW")).thenReturn(true);
        when(authorizationService.hasPermission(actorId, "PRODUCT_VIEW")).thenReturn(true);
        UUID productId = UUID.randomUUID();

        assertDoesNotThrow(() -> tagController.getList(authentication(), null, PageRequest.of(0, 20)));
        assertDoesNotThrow(() -> attributeController.getList(
                authentication(), productId, null, PageRequest.of(0, 20)));

        verify(tagService).getList(null, PageRequest.of(0, 20));
        verify(attributeService).getList(productId, null, PageRequest.of(0, 20));
    }

    @Test
    void storeUserCannotCreateTagEvenWithPermission() {
        authenticate("TAG_CREATE");
        when(authorizationService.hasPermission(actorId, "TAG_CREATE")).thenReturn(true);
        denyGlobal();

        assertThatThrownBy(() -> tagController.create(
                authentication(), ProductTagDto.builder().name("New").build()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(tagService);
    }

    @Test
    void storeUserCannotUpdateAttributeOrProductTagMapping() {
        authenticate("PRODUCT_UPDATE");
        when(authorizationService.hasPermission(actorId, "PRODUCT_UPDATE")).thenReturn(true);
        denyGlobal();
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() -> attributeController.update(authentication(), productId,
                UUID.randomUUID(), ProductAttributeDto.builder()
                        .attributeName("Material").attributeValue("Cotton").build()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> tagController.link(authentication(), productId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(attributeService, mappingService);
    }

    private void denyGlobal() {
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);
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
        @Bean ProductTagService productTagService() { return mock(ProductTagService.class); }
        @Bean ProductAttributeService productAttributeService() { return mock(ProductAttributeService.class); }
        @Bean ProductTagMappingService productTagMappingService() { return mock(ProductTagMappingService.class); }
        @Bean AuthorizationService authorizationService() { return mock(AuthorizationService.class); }
        @Bean UserScopeService userScopeService() { return mock(UserScopeService.class); }
        @Bean ProductAuthorizationService productAuthorizationService(
                AuthorizationService authorizationService, UserScopeService userScopeService) {
            return new ProductAuthorizationService(authorizationService, userScopeService);
        }
        @Bean ProductTagController productTagController(ProductTagService tagService,
                ProductTagMappingService mappingService, ProductAuthorizationService authorization) {
            return new ProductTagController(tagService, mappingService, authorization);
        }
        @Bean ProductAttributeController productAttributeController(
                ProductAttributeService service, ProductAuthorizationService authorization) {
            return new ProductAttributeController(service, authorization);
        }
    }
}
