package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.SupplierDto;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import com.fashionsystem.fashion_system.service.SupplierService;
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

/** API quản lý nhà cung cấp. */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class SupplierController {
    private final SupplierService service;
    private final ProductAuthorizationService authorizationService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    public Page<SupplierDto> getList(Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        authorizationService.requireRead(userId(authentication), "SUPPLIER_VIEW");
        return service.getList(keyword, authorizationService.visibleStatus(userId(authentication), status), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    public SupplierDto getById(Authentication authentication, @PathVariable UUID id) {
        authorizationService.requireRead(userId(authentication), "SUPPLIER_VIEW");
        SupplierDto supplier = service.getById(id);
        authorizationService.requireActiveForStore(userId(authentication),
                "ACTIVE".equalsIgnoreCase(supplier.getStatus()));
        return supplier;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    public SupplierDto create(Authentication authentication, @Valid @RequestBody SupplierDto request) {
        authorizationService.requireMutation(userId(authentication), "SUPPLIER_CREATE");
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    public SupplierDto update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody SupplierDto request) {
        authorizationService.requireMutation(userId(authentication), "SUPPLIER_UPDATE");
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SUPPLIER_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        authorizationService.requireMutation(userId(authentication), "SUPPLIER_DELETE");
        service.delete(id);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    public SupplierDto restore(Authentication authentication, @PathVariable UUID id) {
        authorizationService.requireMutation(userId(authentication), "SUPPLIER_UPDATE");
        return service.restore(id);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
