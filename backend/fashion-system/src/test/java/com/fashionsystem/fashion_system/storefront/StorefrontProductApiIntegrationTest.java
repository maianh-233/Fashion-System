package com.fashionsystem.fashion_system.storefront;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** API storefront qua filter chain thật, khách chưa đăng nhập; dữ liệu mẫu được rollback sau mỗi test. */
@SpringBootTest
@Transactional
class StorefrontProductApiIntegrationTest {
    static final String MARK = "Zqxw";
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    UUID active = UUID.randomUUID();
    UUID stopped = UUID.randomUUID();
    UUID blackM = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        UUID store = jdbc.queryForObject(
                "select id from stores where active = true and latitude is not null and longitude is not null limit 1", UUID.class);
        jdbc.update("insert into products(id, name, code, slug, status, gender, image_url, created_at) values (?, ?, ?, ?, 'ACTIVE', 'FEMALE', '', now())",
                active, "Áo sơ mi " + MARK + " Lụa", "ZQXW-1", "zqxw-ao-so-mi");
        jdbc.update("insert into products(id, name, code, slug, status, gender, image_url, created_at) values (?, ?, ?, ?, 'INACTIVE', 'FEMALE', '', now())",
                stopped, "Áo sơ mi " + MARK + " Cũ", "ZQXW-2", "zqxw-cu");
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, sale_price, active) values (?, ?, 'ZQXW-1-BLK-M', 'Đen', 'M', 500000, 450000, true)",
                blackM, active);
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, 'ZQXW-1-BLK-S', 'Đen', 'S', 500000, true)",
                UUID.randomUUID(), active);
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, 'ZQXW-1-WHT-L', 'Trắng', 'L', 520000, false)",
                UUID.randomUUID(), active);
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, 'ZQXW-2-BLK-M', 'Đen', 'M', 300000, true)",
                UUID.randomUUID(), stopped);
        jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, 0, 7, 0)",
                store, blackM);
    }

    @Test
    void anonymousSearchIgnoresVietnameseAccentsAndHidesStoppedProducts() throws Exception {
        mvc.perform(get("/api/storefront/products").param("keyword", "ao so mi zqxw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("zqxw-ao-so-mi"))
                // Giá bán = giá khuyến mãi niêm yết nếu có (cùng quy tắc lúc thanh toán); biến thể ngừng bán không tính.
                .andExpect(jsonPath("$.content[0].minPrice").value(450000))
                .andExpect(jsonPath("$.content[0].maxPrice").value(500000))
                .andExpect(jsonPath("$.content[0].minOriginalPrice").value(500000))
                .andExpect(jsonPath("$.content[0].onSale").value(true))
                .andExpect(jsonPath("$.content[0].inStock").value(true));
    }

    @Test
    void filtersByGenderPriceAndStock() throws Exception {
        mvc.perform(get("/api/storefront/products").param("keyword", MARK).param("gender", "MALE"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/storefront/products").param("keyword", MARK).param("minPrice", "460000"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/storefront/products").param("keyword", MARK).param("inStock", "true").param("gender", "FEMALE"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/api/storefront/products").param("sort", "bogus"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detailListsSellableVariantsWithOnlineStock() throws Exception {
        mvc.perform(get("/api/storefront/products/zqxw-ao-so-mi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.colors", contains("Đen")))
                .andExpect(jsonPath("$.sizes", contains("S", "M")))
                .andExpect(jsonPath("$.variants[*].sku", containsInAnyOrder("ZQXW-1-BLK-S", "ZQXW-1-BLK-M")))
                .andExpect(jsonPath("$.variants[?(@.sku == 'ZQXW-1-BLK-M')].available", contains(7)))
                .andExpect(jsonPath("$.variants[?(@.sku == 'ZQXW-1-BLK-M')].unitPrice", contains(450000.0)))
                .andExpect(jsonPath("$.inStock").value(true));
        mvc.perform(get("/api/storefront/products/" + active)).andExpect(status().isOk());
        mvc.perform(get("/api/storefront/products/zqxw-cu")).andExpect(status().isNotFound());
    }

    @Test
    void filtersEndpointIsPublicAndWritesStayClosed() throws Exception {
        mvc.perform(get("/api/storefront/products/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genders[*].value", hasItem("FEMALE")))
                .andExpect(jsonPath("$.genders[*].value", not(hasItem("OTHER"))));
        mvc.perform(post("/api/storefront/products")).andExpect(status().isUnauthorized());
        // Danh mục cho nhân viên vẫn cần đăng nhập.
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }
}
