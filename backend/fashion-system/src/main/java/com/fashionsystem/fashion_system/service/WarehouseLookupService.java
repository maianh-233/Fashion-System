package com.fashionsystem.fashion_system.service;

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
    private static final List<String> LOOKUP_PERMISSIONS = List.of("INVENTORY_VIEW", "IMPORT_RECEIPT_VIEW", "IMPORT_RECEIPT_CREATE", "IMPORT_RECEIPT_UPDATE", "EXPORT_RECEIPT_VIEW", "EXPORT_RECEIPT_CREATE", "EXPORT_RECEIPT_UPDATE");

    public Page<ProductOption> products(UUID actor, UUID store, String keyword, Pageable page) {
        access.requireAny(actor,store,LOOKUP_PERMISSIONS);
        return products.search(text(keyword),null,null,null,null,"","",page(page))
            .map(p -> new ProductOption(p.getId(),p.getName(),p.getCode(),p.getCategoryId()));
    }
    public List<VariantOption> variants(UUID actor, UUID store, UUID product) {
        access.requireAny(actor,store,LOOKUP_PERMISSIONS);
        if(!products.existsById(product)) throw BusinessException.notFound("Product not found");
        return variants.searchByProduct(product,"","","",true,null,null,Pageable.unpaged()).stream()
            .map(v -> new VariantOption(v.getId(),v.getProductId(),v.getSku(),v.getColor(),v.getSize(),v.getActive())).toList();
    }
    public Page<SupplierOption> suppliers(UUID actor, UUID store, String keyword, Pageable page) {
        access.requireAny(actor,store,LOOKUP_PERMISSIONS);
        return suppliers.search(text(keyword),"ACTIVE",page(page)).map(s -> new SupplierOption(s.getId(),s.getName(),s.getCode()));
    }
    public List<CategoryOption> categories(UUID actor, UUID store) {
        access.require(actor,store,"INVENTORY_VIEW");
        return categories.search("",null,true,Pageable.unpaged()).stream().map(c -> new CategoryOption(c.getId(),c.getName())).toList();
    }
    private Pageable page(Pageable input) {
        if(input.getPageSize()>100) throw BusinessException.badRequest("Page size cannot exceed 100");
        return PageRequest.of(input.getPageNumber(),input.getPageSize(),Sort.by("name"));
    }
    private String text(String value) { return value==null?"":value.trim(); }
    public record ProductOption(UUID id,String name,String code,UUID categoryId) {}
    public record VariantOption(UUID id,UUID productId,String sku,String color,String size,Boolean active) {}
    public record SupplierOption(UUID id,String name,String code) {}
    public record CategoryOption(UUID id,String name) {}
}
