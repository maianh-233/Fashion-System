package com.fashionsystem.fashion_system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductVariantService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductVariantControllerQueryBindingTest {
    private ProductVariantService variantService;
    private MockMvc mvc;
    private UUID productId;

    @BeforeEach
    void setUp() {
        variantService = mock(ProductVariantService.class);
        ProductAuthorizationService authorizationService = mock(ProductAuthorizationService.class);
        when(authorizationService.visibleActive(any(), any())).thenAnswer(call -> call.getArgument(1));
        mvc = MockMvcBuilders.standaloneSetup(new ProductVariantController(variantService, authorizationService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        productId = UUID.randomUUID();
        when(variantService.getList(eq(productId), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(call -> {
                    Pageable pageable = call.getArgument(7);
                    return new PageImpl<>(List.of(ProductVariantDto.builder()
                            .id(UUID.randomUUID()).productId(productId).size("M")
                            .price(BigDecimal.ONE).active(true).build()), pageable, 1);
                });
    }

    @Test
    void productIdAloneReturnsVariantsWithoutApplyingSizeFilter() throws Exception {
        mvc.perform(get("/api/products/{productId}/variants", productId).principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(variantService).getList(eq(productId), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void pageableSizeDoesNotBecomeVariantSizeFilter() throws Exception {
        mvc.perform(get("/api/products/{productId}/variants", productId)
                        .principal(authentication()).param("page", "0").param("size", "20"))
                .andExpect(status().isOk());

        verify(variantService).getList(eq(productId), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 20));
    }

    @Test
    void variantSizeFiltersVariantsWithoutChangingPageableSize() throws Exception {
        mvc.perform(get("/api/products/{productId}/variants", productId)
                        .principal(authentication()).param("variantSize", "M")
                        .param("page", "0").param("size", "20"))
                .andExpect(status().isOk());

        verify(variantService).getList(eq(productId), isNull(), isNull(), eq("M"), isNull(),
                isNull(), isNull(), org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageNumber() == 0 && pageable.getPageSize() == 20));
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(UUID.randomUUID(), "employee"), null, List.of());
    }
}
