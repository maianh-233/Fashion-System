package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.dto.storefront.StorefrontDtos;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Gợi ý sản phẩm trên cơ sở dữ liệu thật; dữ liệu do test tạo (danh mục / thương hiệu riêng) và rollback. */
@SpringBootTest
@Transactional
class ProductRecommendationIntegrationTest {
    @Autowired ProductRecommendationService recommendations;
    @Autowired JdbcTemplate jdbc;

    final UUID store = UUID.randomUUID();
    final UUID category = UUID.randomUUID();
    final UUID otherCategory = UUID.randomUUID();
    final UUID brand = UUID.randomUUID();
    final UUID shirt = UUID.randomUUID();
    final UUID jeans = UUID.randomUUID();
    final UUID belt = UUID.randomUUID();
    final UUID hat = UUID.randomUUID();
    final UUID sameCategory = UUID.randomUUID();
    final UUID sameBrandOnly = UUID.randomUUID();
    final UUID unrelated = UUID.randomUUID();
    final UUID stopped = UUID.randomUUID();
    String tag;

    @BeforeEach
    void setUp() {
        tag = store.toString().substring(0, 6);
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, 'Zqrc Store', true)", store, "ZQRC-" + tag);
        jdbc.update("insert into categories(id, name, code) values (?, 'Zqrc Áo', ?)", category, "ZQRC-C1-" + tag);
        jdbc.update("insert into categories(id, name, code) values (?, 'Zqrc Khác', ?)", otherCategory, "ZQRC-C2-" + tag);
        jdbc.update("insert into brands(id, name, code) values (?, ?, ?)", brand, "Zqrc Brand " + tag, "ZQRC-B-" + tag);
        product(shirt, category, null, "ACTIVE");
        product(jeans, otherCategory, null, "ACTIVE");
        product(belt, otherCategory, null, "ACTIVE");
        product(hat, otherCategory, null, "ACTIVE");
        product(sameCategory, category, null, "ACTIVE");
        product(sameBrandOnly, otherCategory, brand, "ACTIVE");
        product(unrelated, otherCategory, null, "ACTIVE");
        product(stopped, category, null, "INACTIVE");
        jdbc.update("update products set brand_id = ? where id = ?", brand, shirt);

        order("COMPLETED", shirt, jeans);
        order("COMPLETED", shirt, jeans, belt);
        order("CANCELLED", shirt, hat);
        order("COMPLETED", shirt, stopped);
        order("COMPLETED", sameCategory, unrelated);
    }

    @Test
    void boughtTogetherRanksByBasketsAndSkipsCancelledOrStoppedProducts() {
        StorefrontDtos.Recommendations result = recommendations.recommend(shirt, 8);

        assertThat(result.boughtTogether()).containsExactly(jeans, belt);
    }

    @Test
    void similarPrefersCategoryOverBrandAndNeverRepeats() {
        StorefrontDtos.Recommendations result = recommendations.recommend(shirt, 8);

        assertThat(result.similar()).containsSubsequence(sameCategory, sameBrandOnly)
                .doesNotContain(shirt, jeans, belt, stopped);
        assertThat(result.similar().indexOf(sameCategory)).isLessThan(result.similar().indexOf(sameBrandOnly));
    }

    @Test
    void limitIsClamped() {
        assertThat(recommendations.recommend(shirt, 1).boughtTogether()).containsExactly(jeans);
        assertThat(recommendations.recommend(UUID.randomUUID(), 8).boughtTogether()).isEmpty();
    }

    private void product(UUID id, UUID categoryId, UUID brandId, String status) {
        String code = "ZQRC-" + id.toString().substring(0, 8);
        jdbc.update("insert into products(id, code, name, slug, status, image_url, category_id, brand_id) values (?, ?, ?, ?, ?, '', ?, ?)",
                id, code, "Zqrc " + code, code.toLowerCase(), status, categoryId, brandId);
        jdbc.update("insert into product_variants(id, product_id, sku, size, price, active) values (?, ?, ?, 'M', 200000, true)",
                UUID.randomUUID(), id, code + "-M");
    }

    private void order(String status, UUID... products) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into orders(id, order_code, order_type, status, active, store_id, subtotal, discount_total, shipping_fee, "
                + "total_amount, payment_status, created_at) values (?, ?, 'OFFLINE', ?, true, ?, 0, 0, 0, 0, 'PAID', now() at time zone 'UTC')",
                id, "ZQRC-" + id.toString().substring(0, 12), status, store);
        for (UUID productId : products) {
            UUID variant = jdbc.queryForObject("select id from product_variants where product_id = ?", UUID.class, productId);
            jdbc.update("insert into order_items(order_id, product_id, product_variant_id, product_name, price, quantity, total) "
                    + "values (?, ?, ?, 'Zqrc', 200000, 1, 200000)", id, productId, variant);
        }
    }
}
