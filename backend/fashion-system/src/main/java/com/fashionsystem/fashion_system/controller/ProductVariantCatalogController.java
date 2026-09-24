package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductVariantService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Paginated variant catalog across Products, optionally restricted to one Product. */
@RestController
@RequestMapping("/api/product-variants")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ProductVariantCatalogController {
    private final ProductVariantService variantService;
    private final ProductAuthorizationService authorizationService;

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_VIEW')")
    public Page<ProductVariantDto> list(Authentication authentication,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String variantSize,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "productId") Pageable pageable) {
        authorizationService.requireRead(((AuthenticatedUser) authentication.getPrincipal()).userId(),
                "PRODUCT_VARIANT_VIEW");
        return variantService.getAll(productId, keyword, color, variantSize,
                authorizationService.visibleActive(((AuthenticatedUser) authentication.getPrincipal()).userId(), active), pageable);
    }
}
