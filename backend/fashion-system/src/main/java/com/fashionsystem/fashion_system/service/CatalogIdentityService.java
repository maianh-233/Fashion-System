package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.exception.BusinessException;
import java.text.Normalizer;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Allocates catalog identities through PostgreSQL sequences shared by all application instances. */
@Service
@RequiredArgsConstructor
public class CatalogIdentityService {
    private final JdbcTemplate jdbc;

    public String nextBrandCode() { return code("catalog_brand_code_seq", "BR", 6); }
    public String nextCategoryCode() { return code("catalog_category_code_seq", "CAT", 6); }
    public String nextCollectionCode() { return code("catalog_collection_code_seq", "COL", 6); }
    public String nextProductCode() { return code("catalog_product_code_seq", "PRD", 6); }
    public String nextSku() { return code("catalog_variant_sku_seq", "SKU", 9); }

    private String code(String sequence, String prefix, int digits) {
        Long value = jdbc.queryForObject("select nextval('" + sequence + "')", Long.class);
        if (value == null) throw BusinessException.invalidState("Không thể sinh mã catalog");
        return prefix + String.format(Locale.ROOT, "%0" + digits + "d", value);
    }

    public String slugBase(String name) {
        String ascii = Normalizer.normalize(name.trim().replace('đ', 'd').replace('Đ', 'D'),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) throw BusinessException.badRequest("Tên sản phẩm không tạo được slug hợp lệ");
        return slug;
    }
}
