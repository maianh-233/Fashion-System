package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class WarehouseLookupController {
    private final StoreAccessService access;
    private final WarehouseLookupService lookups;

    @GetMapping("/stores")
    public List<StoreAccessService.StoreOption> stores(Authentication auth) { return access.accessibleStores(actor(auth)); }
    @GetMapping("/products")
    public Page<WarehouseLookupService.ProductOption> products(Authentication auth,@RequestParam UUID storeId,
            @RequestParam(defaultValue="") String keyword,@PageableDefault(size=20) Pageable page) {
        return lookups.products(actor(auth),storeId,keyword,page);
    }
    @GetMapping("/products/{productId}/variants")
    public List<WarehouseLookupService.VariantOption> variants(Authentication auth,@RequestParam UUID storeId,@PathVariable UUID productId) {
        return lookups.variants(actor(auth),storeId,productId);
    }
    @GetMapping("/suppliers")
    public Page<WarehouseLookupService.SupplierOption> suppliers(Authentication auth,@RequestParam UUID storeId,
            @RequestParam(defaultValue="") String keyword,@PageableDefault(size=20) Pageable page) {
        return lookups.suppliers(actor(auth),storeId,keyword,page);
    }
    @GetMapping("/categories")
    public List<WarehouseLookupService.CategoryOption> categories(Authentication auth,@RequestParam UUID storeId) {
        return lookups.categories(actor(auth),storeId);
    }
    private UUID actor(Authentication auth) { return ((AuthenticatedUser)auth.getPrincipal()).userId(); }
}
