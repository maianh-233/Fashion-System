package com.fashionsystem.fashion_system.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fashionsystem.fashion_system.service.ProductVariantService;
import org.springframework.data.domain.PageImpl;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@SpringBootTest
class ProductVariantCatalogQueryTest {
    @Autowired ProductVariantRepository variants;
    @Autowired ProductVariantService service;
    @Autowired ObjectMapper objectMapper;

    @Test
    void pageUsesStablePagedModelJson() {
        String json = objectMapper.writeValueAsString(new PageImpl<>(java.util.List.of("item")));
        assertTrue(json.contains("\"content\""));
        assertTrue(json.contains("\"page\""));
    }

    @Test
    void catalogQueryListsExistingVariantsWithoutSelectedProduct() {
        var page = variants.searchAll(null, "", "", "", null,
                PageRequest.of(0, 20, Sort.by("productId")));
        if (variants.count() > 0) {
            assertTrue(page.getTotalElements() > 0);
            var catalog = service.getAll(null, "", "", "", null,
                    PageRequest.of(0, 20, Sort.by("productId")));
            assertTrue(catalog.getTotalElements() > 0);
            var first = catalog.getContent().get(0);
            assertTrue(service.getList(first.getProductId(), "", "", "", null,
                    null, null, PageRequest.of(0, 20, Sort.by("sku"))).getTotalElements() > 0);
        } else {
            assertEquals(0, page.getTotalElements());
        }
    }
}
