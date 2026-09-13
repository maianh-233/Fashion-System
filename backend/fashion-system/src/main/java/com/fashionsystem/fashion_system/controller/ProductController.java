package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductService;
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

/** Internal Product master API with RBAC plus Global mutation scope. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ProductController {
    private final ProductService productService;
    private final ProductAuthorizationService productAuthorizationService;

    /** Creates a chain-wide Product master record. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public ProductDto create(Authentication authentication, @Valid @RequestBody ProductDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_CREATE");
        return productService.create(request);
    }

    /** Returns Product detail to an authorized internal employee. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    public ProductDto getById(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VIEW");
        return productService.getById(id);
    }

    /** Searches the global Product catalog with database pagination. */
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    public Page<ProductDto> getList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID collectionId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VIEW");
        return productService.getList(
                keyword, brandId, collectionId, categoryId, gender, status, pageable);
    }

    /** Updates a chain-wide Product master record. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ProductDto update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody ProductDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_UPDATE");
        return productService.update(id, request);
    }

    /** Deletes a Product master record when existing business references permit deletion. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_DELETE");
        productService.delete(id);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
