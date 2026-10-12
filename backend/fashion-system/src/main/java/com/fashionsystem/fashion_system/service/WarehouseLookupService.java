package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheKeys;
import com.fashionsystem.fashion_system.dto.WarehouseLookupDto.CategoryOption;
import com.fashionsystem.fashion_system.dto.WarehouseLookupDto.ProductOption;
import com.fashionsystem.fashion_system.dto.WarehouseLookupDto.SupplierOption;
import com.fashionsystem.fashion_system.dto.WarehouseLookupDto.VariantOption;
import com.fashionsystem.fashion_system.repository.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Minimal catalog projections for warehouse operators; never grants catalog editing privileges. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseLookupService {
    private final StoreAccessService access;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final SupplierRepository suppliers;
    private final CategoryRepository categories;
    private final CatalogReadCacheService cacheReads;
    private static final List<String> LOOKUP_PERMISSIONS = List.of("INVENTORY_VIEW", "IMPORT_RECEIPT_VIEW", "IMPORT_RECEIPT_CREATE", "IMPORT_RECEIPT_UPDATE", "EXPORT_RECEIPT_VIEW", "EXPORT_RECEIPT_CREATE", "EXPORT_RECEIPT_UPDATE");

    public Page<ProductOption> products(UUID actor, UUID store, String keyword, Pageable page) {
        UUID resolvedStore = access.requireAny(actor,store,LOOKUP_PERMISSIONS);
        String normalizedKeyword = text(keyword);
        Pageable effectivePage = page(page);
        return cacheReads.warehouseProducts(CacheKeys.of(resolvedStore, normalizedKeyword, effectivePage),
                () -> products.search(normalizedKeyword,null,null,null,null,"","",effectivePage)
                        .map(p -> new ProductOption(p.getId(),p.getName(),p.getCode(),p.getCategoryId())))
                .toPage();
    }
    public List<VariantOption> variants(UUID actor, UUID store, UUID product) {
        UUID resolvedStore = access.requireAny(actor,store,LOOKUP_PERMISSIONS);
        if(!products.existsById(product)) throw BusinessException.notFound("Product not found");
        return cacheReads.warehouseVariants(CacheKeys.of(resolvedStore, product),
                () -> variants.searchByProduct(product,"","","",true,null,null,Pageable.unpaged()).stream()
                        .map(v -> new VariantOption(v.getId(),v.getProductId(),v.getSku(),v.getColor(),v.getSize(),v.getActive())).toList());
    }
    public Page<SupplierOption> suppliers(UUID actor, UUID store, String keyword, Pageable page) {
        UUID resolvedStore = access.requireAny(actor,store,LOOKUP_PERMISSIONS);
        String normalizedKeyword = text(keyword);
        Pageable effectivePage = page(page);
        return cacheReads.warehouseSuppliers(CacheKeys.of(resolvedStore, normalizedKeyword, effectivePage),
                () -> suppliers.search(normalizedKeyword,"ACTIVE",effectivePage)
                        .map(s -> new SupplierOption(s.getId(),s.getName(),s.getCode())))
                .toPage();
    }
    public List<CategoryOption> categories(UUID actor, UUID store) {
        UUID resolvedStore = access.require(actor,store,"INVENTORY_VIEW");
        return cacheReads.warehouseCategories(CacheKeys.of(resolvedStore),
                () -> categories.search("",null,true,Pageable.unpaged()).stream()
                        .map(c -> new CategoryOption(c.getId(),c.getName())).toList());
    }
    private Pageable page(Pageable input) {
        if(input.getPageSize()>100) throw BusinessException.badRequest("Page size cannot exceed 100");
        return PageRequest.of(input.getPageNumber(),input.getPageSize(),Sort.by("name"));
    }
    private String text(String value) { return value==null?"":value.trim(); }
}
