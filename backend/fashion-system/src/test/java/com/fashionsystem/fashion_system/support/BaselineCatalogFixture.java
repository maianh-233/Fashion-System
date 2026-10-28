package com.fashionsystem.fashion_system.support;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Minimal catalogue for integration tests that read "the first active store / variant" from the
 * database. A developer database already has this data and is left untouched; an empty database
 * built by Flyway (CI) gets two stores, a supplier, one product with two variants and stock for
 * both at the first store. Call it from a {@code @Transactional} test so the rows roll back.
 */
public final class BaselineCatalogFixture {
    private BaselineCatalogFixture() {
    }

    public static void ensure(JdbcTemplate jdbc) {
        Integer stores = jdbc.queryForObject("select count(*) from stores where active = true", Integer.class);
        if (stores != null && stores >= 2) return;

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, 'ZQCI-S1', 'Zqci Store 1', true, 10.77, 106.70)", first);
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, 'ZQCI-S2', 'Zqci Store 2', true, 21.03, 105.85)", second);
        jdbc.update("insert into suppliers(id, code, name) values (?, 'ZQCI-SUP', 'Zqci Supplier')", UUID.randomUUID());

        UUID product = UUID.randomUUID();
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, 'ZQCI-P1', 'Zqci Áo', 'zqci-ao', 'ACTIVE', '')", product);
        for (String size : new String[] {"M", "S"}) {
            UUID variant = UUID.randomUUID();
            jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Đen', ?, 300000, true)",
                    variant, product, "ZQCI-P1-" + size, size);
            jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, 20, 10, 0)",
                    first, variant);
        }
    }
}
