package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.ProductAttributeDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAttributeService;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Nested Product Attribute API using Product read/update authorization. */
@RestController
@RequestMapping("/api/products/{productId}/attributes")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ProductAttributeController {
    private final ProductAttributeService attributeService;
    private final ProductAuthorizationService productAuthorizationService;

    /** Creates shared Product metadata. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ProductAttributeDto create(Authentication authentication, @PathVariable UUID productId,
            @Valid @RequestBody ProductAttributeDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_UPDATE");
        return attributeService.create(productId, request);
    }

    /** Reads one Product Attribute. */
    @GetMapping("/{attributeId}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    public ProductAttributeDto getById(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID attributeId) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VIEW");
        return attributeService.getById(productId, attributeId);
    }

    /** Lists Product Attributes using database pagination. */
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    public Page<ProductAttributeDto> getList(Authentication authentication, @PathVariable UUID productId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "attributeName") Pageable pageable) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VIEW");
        return attributeService.getList(productId, keyword, pageable);
    }

    /** Updates shared Product metadata. */
    @PutMapping("/{attributeId}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ProductAttributeDto update(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID attributeId, @Valid @RequestBody ProductAttributeDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_UPDATE");
        return attributeService.update(productId, attributeId, request);
    }

    /** Deletes shared Product metadata. */
    @DeleteMapping("/{attributeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public void delete(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID attributeId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_UPDATE");
        attributeService.delete(productId, attributeId);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
