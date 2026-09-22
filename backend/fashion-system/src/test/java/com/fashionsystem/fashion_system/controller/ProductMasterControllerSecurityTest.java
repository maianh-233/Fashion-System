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

import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AuthorizationService;
import com.fashionsystem.fashion_system.service.BrandService;
import com.fashionsystem.fashion_system.service.CategoryService;
import com.fashionsystem.fashion_system.service.CollectionService;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.UserScopeService;
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

/** Verifies Product master controllers require both RBAC authority and service-enforced Global scope. */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductMasterControllerSecurityTest.TestConfiguration.class)
class ProductMasterControllerSecurityTest {
    @Autowired BrandController controller;
    @Autowired BrandService brandService;
    @Autowired CategoryController categoryController;
    @Autowired CategoryService categoryService;
    @Autowired CollectionController collectionController;
    @Autowired CollectionService collectionService;
    @Autowired AuthorizationService authorizationService;
    @Autowired UserScopeService userScopeService;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        clearInvocations(brandService, categoryService, collectionService,
                authorizationService, userScopeService);
        actorId = UUID.randomUUID();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void storeUserWithBrandCreateAuthorityAndPermissionIsStillDenied() {
        authenticate("BRAND_CREATE");
        when(authorizationService.hasPermission(actorId, "BRAND_CREATE")).thenReturn(true);
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);

        assertThatThrownBy(() -> controller.create(authentication(), validBrand()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(brandService);
    }

    @Test
    void missingBrandCreateAuthorityIsDeniedBeforeBusinessServices() {
        authenticate("BRAND_VIEW");

        assertThatThrownBy(() -> controller.create(authentication(), validBrand()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(brandService, authorizationService, userScopeService);
    }

    @Test
    void globalUserWithPermissionCanCreateBrand() {
        authenticate("BRAND_CREATE");
        when(authorizationService.hasPermission(actorId, "BRAND_CREATE")).thenReturn(true);

        assertDoesNotThrow(() -> controller.create(authentication(), validBrand()));

        verify(userScopeService).requireGlobal(actorId);
        verify(brandService).create(any(BrandDto.class));
    }

    @Test
    void storeUserCannotCreateCategory() {
        authenticate("CATEGORY_CREATE");
        when(authorizationService.hasPermission(actorId, "CATEGORY_CREATE")).thenReturn(true);
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);

        assertThatThrownBy(() -> categoryController.create(authentication(),
                CategoryDto.builder().name("Áo").code("AO").build()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(categoryService);
    }

    @Test
    void storeUserCannotUpdateCollection() {
        authenticate("COLLECTION_UPDATE");
        when(authorizationService.hasPermission(actorId, "COLLECTION_UPDATE")).thenReturn(true);
        doThrow(BusinessException.forbidden("Chỉ nhân viên cấp chuỗi được thay đổi dữ liệu sản phẩm"))
                .when(userScopeService).requireGlobal(actorId);

        assertThatThrownBy(() -> collectionController.update(authentication(), UUID.randomUUID(),
                CollectionDto.builder().name("Fall").season("FALL").year(2026).build()))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(collectionService);
    }

    private BrandDto validBrand() {
        return BrandDto.builder().name("Lunaria").code("LUNARIA").status("ACTIVE").build();
    }

    private void authenticate(String... authorities) {
        var granted = java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(actorId, "employee"), null, granted));
    }

    private org.springframework.security.core.Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean BrandService brandService() { return mock(BrandService.class); }
        @Bean CategoryService categoryService() { return mock(CategoryService.class); }
        @Bean CollectionService collectionService() { return mock(CollectionService.class); }
        @Bean AuthorizationService authorizationService() { return mock(AuthorizationService.class); }
        @Bean UserScopeService userScopeService() { return mock(UserScopeService.class); }
        @Bean ProductAuthorizationService productAuthorizationService(
                AuthorizationService authorizationService, UserScopeService userScopeService) {
            return new ProductAuthorizationService(authorizationService, userScopeService);
        }
        @Bean BrandController brandController(
                BrandService brandService, ProductAuthorizationService productAuthorizationService) {
            return new BrandController(brandService, productAuthorizationService,
                    mock(com.fashionsystem.fashion_system.service.CatalogMediaService.class));
        }
        @Bean CategoryController categoryController(
                CategoryService categoryService, ProductAuthorizationService productAuthorizationService) {
            return new CategoryController(categoryService, productAuthorizationService);
        }
        @Bean CollectionController collectionController(
                CollectionService collectionService, ProductAuthorizationService productAuthorizationService) {
            return new CollectionController(collectionService, productAuthorizationService,
                    mock(com.fashionsystem.fashion_system.service.CatalogMediaService.class));
        }
    }
}
