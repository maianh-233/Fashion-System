package com.fashionsystem.fashion_system.storefront;

import static org.hamcrest.Matchers.containsInAnyOrder;
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

/** Thương hiệu, bộ sưu tập, khuyến mãi công khai; dữ liệu mẫu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class StorefrontBrowseApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    final UUID brand = UUID.randomUUID();
    final UUID stoppedBrand = UUID.randomUUID();
    final UUID released = UUID.randomUUID();
    final UUID draft = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID productPromo = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("insert into brands(id, name, code, status) values (?, 'Zqxw Atelier', 'ZQXW_B1', 'ACTIVE')", brand);
        jdbc.update("insert into brands(id, name, code, status) values (?, 'Zqxw Cũ', 'ZQXW_B2', 'INACTIVE')", stoppedBrand);
        jdbc.update("insert into collections(id, brand_id, name, code, season, year, status) values (?, ?, 'Zqxw Thu', 'ZQXW_C1', 'AUTUMN', 2026, 'ACTIVE')", released, brand);
        jdbc.update("insert into collections(id, brand_id, name, code, status) values (?, ?, 'Zqxw Nháp', 'ZQXW_C2', 'DRAFT')", draft, brand);
        jdbc.update("insert into products(id, brand_id, collection_id, name, code, slug, status, image_url) values (?, ?, ?, 'Zqxw áo', 'ZQXW-P1', 'zqxw-p1', 'ACTIVE', '')",
                product, brand, released);
        jdbc.update("insert into product_variants(id, product_id, sku, price, active) values (?, ?, 'ZQXW-P1-M', 300000, true)", UUID.randomUUID(), product);
        promotion(UUID.randomUUID(), "ZQXW_ORDER", "ORDER", "ACTIVE", "now() - interval '1 day'", "now() + interval '10 day'");
        promotion(productPromo, "ZQXW_PRODUCT", "PRODUCT", "SCHEDULED", "now() + interval '2 day'", "now() + interval '20 day'");
        promotion(UUID.randomUUID(), "ZQXW_OFF", "ORDER", "INACTIVE", "now() - interval '1 day'", "now() + interval '10 day'");
        promotion(UUID.randomUUID(), "ZQXW_OLD", "ORDER", "ACTIVE", "now() - interval '9 day'", "now() - interval '1 day'");
        jdbc.update("insert into promotion_products(promotion_id, product_id) values (?, ?)", productPromo, product);
    }

    private void promotion(UUID id, String code, String type, String status, String start, String end) {
        jdbc.update("insert into promotions(id, code, name, promotion_type, discount_type, discount_value, start_date, end_date, status) "
                + "values (?, ?, ?, ?, 'PERCENT', 10, " + start + ", " + end + ", ?)", id, code, "Zqxw " + code, type, status);
    }

    @Test
    void brandsAreActiveOnlyWithSellableCounts() throws Exception {
        mvc.perform(get("/api/storefront/brands").param("keyword", "zqxw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productCount").value(1))
                .andExpect(jsonPath("$.content[0].collectionCount").value(1));
        mvc.perform(get("/api/storefront/brands/" + stoppedBrand)).andExpect(status().isNotFound());
        mvc.perform(get("/api/storefront/products").param("brandId", brand.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void draftCollectionsAreHidden() throws Exception {
        mvc.perform(get("/api/storefront/collections").param("brandId", brand.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Zqxw Thu"))
                .andExpect(jsonPath("$.content[0].brand.name").value("Zqxw Atelier"))
                .andExpect(jsonPath("$.content[0].productCount").value(1));
        mvc.perform(get("/api/storefront/collections/" + draft)).andExpect(status().isNotFound());
        mvc.perform(get("/api/storefront/products").param("collectionId", released.toString()))
                .andExpect(jsonPath("$.content[0].slug").value("zqxw-p1"));
    }

    @Test
    void promotionsShowCurrentAndUpcomingOnly() throws Exception {
        mvc.perform(get("/api/storefront/promotions").param("keyword", "zqxw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", containsInAnyOrder("ZQXW_ORDER", "ZQXW_PRODUCT")));
        mvc.perform(get("/api/storefront/promotions").param("keyword", "zqxw").param("type", "PRODUCT"))
                .andExpect(jsonPath("$.content[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.content[0].productCount").value(1));
        mvc.perform(get("/api/storefront/products").param("promotionId", productPromo.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/api/storefront/promotions").param("type", "FREESHIP")).andExpect(status().isBadRequest());
    }
}
