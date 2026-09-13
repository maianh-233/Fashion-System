package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductImageService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Secured Product image API backed by server-mediated Cloudinary uploads. */
@RestController
@RequestMapping("/api/products/{productId}/variants/{variantId}/images")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ProductImageController {
    private final ProductImageService imageService;
    private final ProductAuthorizationService productAuthorizationService;

    /** Uploads image content without accepting a client URL or Cloudinary public id. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public ProductImageDto upload(Authentication authentication,
            @PathVariable UUID productId, @PathVariable UUID variantId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean isPrimary,
            @RequestParam(defaultValue = "0") int sortOrder) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        return imageService.upload(productId, variantId, file, isPrimary, sortOrder);
    }

    /** Lists images for a readable Variant. */
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_VIEW')")
    public List<ProductImageDto> getList(Authentication authentication,
            @PathVariable UUID productId, @PathVariable UUID variantId) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VARIANT_VIEW");
        return imageService.getList(productId, variantId);
    }

    /** Returns one image for a readable Variant. */
    @GetMapping("/{imageId}")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_VIEW')")
    public ProductImageDto getById(Authentication authentication,
            @PathVariable UUID productId, @PathVariable UUID variantId, @PathVariable UUID imageId) {
        productAuthorizationService.requireRead(userId(authentication), "PRODUCT_VARIANT_VIEW");
        return imageService.getById(productId, variantId, imageId);
    }

    /** Replaces binary content while keeping server-owned provider identifiers. */
    @PutMapping(path = "/{imageId}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public ProductImageDto replaceContent(Authentication authentication,
            @PathVariable UUID productId, @PathVariable UUID variantId, @PathVariable UUID imageId,
            @RequestPart("file") MultipartFile file) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        return imageService.replaceContent(productId, variantId, imageId, file);
    }

    /** Updates primary/sort metadata without accepting URL or public id. */
    @PatchMapping("/{imageId}")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public ProductImageDto updateMetadata(Authentication authentication,
            @PathVariable UUID productId, @PathVariable UUID variantId, @PathVariable UUID imageId,
            @RequestParam(defaultValue = "false") boolean isPrimary,
            @RequestParam(defaultValue = "0") int sortOrder) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        return imageService.updateMetadata(productId, variantId, imageId, isPrimary, sortOrder);
    }

    /** Sets the unique primary image for a Variant. */
    @PatchMapping("/{imageId}/primary")
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public ProductImageDto setPrimary(Authentication authentication,
            @PathVariable UUID productId, @PathVariable UUID variantId, @PathVariable UUID imageId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        return imageService.setPrimary(productId, variantId, imageId);
    }

    /** Deletes the persisted Cloudinary asset and Product image association. */
    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRODUCT_VARIANT_UPDATE')")
    public void delete(Authentication authentication,
            @PathVariable UUID productId, @PathVariable UUID variantId, @PathVariable UUID imageId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_VARIANT_UPDATE");
        imageService.delete(productId, variantId, imageId);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
