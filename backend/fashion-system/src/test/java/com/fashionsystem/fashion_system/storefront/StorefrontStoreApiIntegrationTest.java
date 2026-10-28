package com.fashionsystem.fashion_system.storefront;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Hệ thống cửa hàng và cửa hàng còn hàng công khai; dữ liệu mẫu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class StorefrontStoreApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    final UUID open = UUID.randomUUID();
    final UUID low = UUID.randomUUID();
    final UUID closed = UUID.randomUUID();
    final UUID onlineOnly = UUID.randomUUID();
    final UUID variant = UUID.randomUUID();
    final UUID stoppedVariant = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        store(open, "ZQXW_S1", "Zqxw Mở", true, 1.111111, "{\"Styling riêng\",\"Đổi trả\"}");
        store(low, "ZQXW_S2", "Zqxw Sắp hết", true, 1.222222, "{}");
        store(closed, "ZQXW_S3", "Zqxw Đóng", false, 1.333333, "{}");
        store(onlineOnly, "ZQXW_S4", "Zqxw Online", true, 1.444444, "{}");
        UUID product = UUID.randomUUID();
        jdbc.update("insert into products(id, name, code, slug, status, image_url) values (?, 'Zqxw áo', 'ZQXW-P9', 'zqxw-p9', 'ACTIVE', '')", product);
        jdbc.update("insert into product_variants(id, product_id, sku, size, price, active) values (?, ?, 'ZQXW-P9-M', 'M', 300000, true)", variant, product);
        jdbc.update("insert into product_variants(id, product_id, sku, size, price, active) values (?, ?, 'ZQXW-P9-L', 'L', 300000, false)", stoppedVariant, product);
        balance(open, variant, 5, 0);
        balance(low, variant, 2, 0);
        balance(closed, variant, 9, 0);
        balance(onlineOnly, variant, 0, 7);
        balance(open, stoppedVariant, 5, 0);
    }

    private void store(UUID id, String code, String name, boolean active, double latitude, String services) {
        jdbc.update("insert into stores(id, code, name, address, phone, latitude, longitude, active, opening_hours, services) "
                + "values (?, ?, ?, 'Đường Zqxw, Hà Nội', '0901234567', ?, 1.5, ?, '09:00 – 22:00', ?::text[])",
                id, code, name, latitude, active, services);
    }

    private void balance(UUID store, UUID variantId, int offline, int online) {
        jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) "
                + "values (?, ?, ?, ?, 0)", store, variantId, offline, online);
    }

    @Test
    void activeStoresWithStorefrontDetails() throws Exception {
        mvc.perform(get("/api/storefront/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'ZQXW_S1')].services[0]").value("Styling riêng"))
                .andExpect(jsonPath("$[?(@.code == 'ZQXW_S1')].openingHours").value("09:00 – 22:00"))
                .andExpect(jsonPath("$[*].code", not(hasItem("ZQXW_S3"))));
    }

    @Test
    void availabilityUsesInStoreStockWithoutQuantities() throws Exception {
        mvc.perform(get("/api/storefront/variants/" + variant + "/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.store.code == 'ZQXW_S1')].stock").value("IN_STOCK"))
                .andExpect(jsonPath("$[?(@.store.code == 'ZQXW_S2')].stock").value("LOW_STOCK"))
                .andExpect(jsonPath("$[0].quantity").doesNotExist());
    }

    @Test
    void stoppedVariantIsNotFound() throws Exception {
        mvc.perform(get("/api/storefront/variants/" + stoppedVariant + "/stores")).andExpect(status().isNotFound());
        mvc.perform(get("/api/storefront/variants/" + UUID.randomUUID() + "/stores")).andExpect(status().isNotFound());
    }
}
