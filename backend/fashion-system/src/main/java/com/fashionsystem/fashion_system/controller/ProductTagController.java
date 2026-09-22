package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.dto.ProductTagDto;
import com.fashionsystem.fashion_system.dto.ProductTagMappingDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.ProductTagMappingService;
import com.fashionsystem.fashion_system.service.ProductTagService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Product Tag catalog and Product-to-Tag mapping API. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class ProductTagController {
    private final ProductTagService tagService;
    private final ProductTagMappingService mappingService;
    private final ProductAuthorizationService productAuthorizationService;

    /** Creates a global Product Tag. */
    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TAG_CREATE')")
    public ProductTagDto create(Authentication authentication, @Valid @RequestBody ProductTagDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "TAG_CREATE");
        return tagService.create(request);
    }

    /** Reads one Product Tag. */
    @GetMapping("/tags/{id}")
    @PreAuthorize("hasAuthority('TAG_VIEW')")
    public ProductTagDto getById(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireRead(userId(authentication), "TAG_VIEW");
        ProductTagDto tag = tagService.getById(id);
        productAuthorizationService.requireActiveForStore(userId(authentication), Boolean.TRUE.equals(tag.getActive()));
        return tag;
    }

    /** Searches global Product Tags with database pagination. */
    @GetMapping("/tags")
    @PreAuthorize("hasAuthority('TAG_VIEW')")
    public Page<ProductTagDto> getList(Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        productAuthorizationService.requireRead(userId(authentication), "TAG_VIEW");
        return tagService.getList(keyword,
                productAuthorizationService.visibleActive(userId(authentication), active), pageable);
    }

    /** Updates a global Product Tag. */
    @PutMapping("/tags/{id}")
    @PreAuthorize("hasAuthority('TAG_UPDATE')")
    public ProductTagDto update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody ProductTagDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "TAG_UPDATE");
        return tagService.update(id, request);
    }

    /** Deletes an unused global Product Tag. */
    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TAG_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireMutation(userId(authentication), "TAG_DELETE");
        tagService.delete(id);
    }

    @PatchMapping("/tags/{id}/restore")
    @PreAuthorize("hasAuthority('TAG_UPDATE')")
    public ProductTagDto restore(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireMutation(userId(authentication), "TAG_UPDATE");
        return tagService.restore(id);
    }

    /** Links a Tag to a Product; this mutates the global Product master. */
    @PostMapping("/products/{productId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ProductTagMappingDto link(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID tagId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_UPDATE");
        return mappingService.link(productId, tagId);
    }

    /** Unlinks a Tag from a Product. */
    @DeleteMapping("/products/{productId}/tags/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public void unlink(Authentication authentication, @PathVariable UUID productId,
            @PathVariable UUID tagId) {
        productAuthorizationService.requireMutation(userId(authentication), "PRODUCT_UPDATE");
        mappingService.unlink(productId, tagId);
    }

    /** Lists Tags attached to a readable Product. */
    @GetMapping("/products/{productId}/tags")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW') and hasAuthority('TAG_VIEW')")
    public Page<ProductTagDto> getProductTags(Authentication authentication,
            @PathVariable UUID productId, @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        UUID actorId = userId(authentication);
        productAuthorizationService.requireRead(actorId, "PRODUCT_VIEW");
        productAuthorizationService.requireRead(actorId, "TAG_VIEW");
        return mappingService.getTags(productId, pageable);
    }

    /** Lists Products attached to a readable Tag. */
    @GetMapping("/tags/{tagId}/products")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW') and hasAuthority('TAG_VIEW')")
    public Page<ProductDto> getTaggedProducts(Authentication authentication,
            @PathVariable UUID tagId, @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        UUID actorId = userId(authentication);
        productAuthorizationService.requireRead(actorId, "PRODUCT_VIEW");
        productAuthorizationService.requireRead(actorId, "TAG_VIEW");
        return mappingService.getProducts(tagId, pageable);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
