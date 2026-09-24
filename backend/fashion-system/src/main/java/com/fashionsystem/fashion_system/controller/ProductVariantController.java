package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductVariantService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Nested Product Variant API; Store employees are read-only regardless of RBAC mutation grants. */
@RestController
@RequestMapping("/api/products/{productId}/variants")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ProductVariantController {
    private final ProductVariantService variantService;
    private final ProductAuthorizationService productAuthorizationService;

    /** Creates a Variant under the global Product. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_CREATE')")
    public ProductVariantDto create(Authentication authentication, @PathVariable UUID productId,
            @Valid @RequestBody ProductVariantDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_CREATE");
        return variantService.create(productId, request);
    }

    /** Returns one Variant belonging to the supplied Product. */
    @GetMapping("/{variantId}")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_VIEW')")
    public ProductVariantDto getById(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VARIANT_VIEW");
        ProductVariantDto variant = variantService.getById(productId, variantId);
        productAuthorizationService.requireActiveForStore(userId(authentication), Boolean.TRUE.equals(variant.getActive()));
        return variant;
    }

    /** Searches Variants inside one Product with database pagination. */
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_VIEW')")
    public Page<ProductVariantDto> getList(
            Authentication authentication,
            @PathVariable UUID productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String variantSize,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "sku") Pageable pageable) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VARIANT_VIEW");
        return variantService.getList(
                productId, keyword, color, variantSize,
                productAuthorizationService.visibleActive(userId(authentication), active), minPrice, maxPrice, pageable);
    }

    /** Updates SKU, barcode, pricing, and shared Variant metadata. */
    @PutMapping("/{variantId}")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public ProductVariantDto update(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID variantId, @Valid @RequestBody ProductVariantDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        return variantService.update(productId, variantId, request);
    }

    /** Deletes an unused Variant. */
    @DeleteMapping("/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_DELETE");
        variantService.delete(productId, variantId);
    }

    /** Activates a Variant; this remains a Global Product mutation. */
    @PatchMapping("/{variantId}/activate")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public ProductVariantDto activate(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        return variantService.activate(productId, variantId);
    }

    /** Deactivates a Variant; this remains a Global Product mutation. */
    @PatchMapping("/{variantId}/deactivate")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public ProductVariantDto deactivate(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        return variantService.deactivate(productId, variantId);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
