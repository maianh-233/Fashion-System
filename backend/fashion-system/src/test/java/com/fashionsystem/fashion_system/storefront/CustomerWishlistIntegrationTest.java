package com.fashionsystem.fashion_system.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.dto.storefront.WishlistDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.service.CustomerWishlistService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Danh sách yêu thích trên cơ sở dữ liệu thật; dữ liệu mẫu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class CustomerWishlistIntegrationTest {
    @Autowired CustomerWishlistService wishlist;
    @Autowired JdbcTemplate jdbc;
    final UUID customer = UUID.randomUUID();
    final UUID active = UUID.randomUUID();
    final UUID second = UUID.randomUUID();
    final UUID stopped = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.update("insert into customers(id, customer_code, membership_status, source) values (?, 'ZQXW-KH', 'MEMBER', 'WEBSITE')", customer);
        product(active, "ZQXW-W1", "ACTIVE");
        product(second, "ZQXW-W2", "ACTIVE");
        product(stopped, "ZQXW-W3", "INACTIVE");
    }

    private void product(UUID id, String code, String status) {
        jdbc.update("insert into products(id, name, code, slug, status, image_url) values (?, ?, ?, ?, ?, '')",
                id, "Zqxw " + code, code, code.toLowerCase(), status);
    }

    @Test
    void addIsIdempotentAndNewestFirst() {
        wishlist.add(customer, active);
        wishlist.add(customer, active);
        jdbc.update("update wishlist_items set created_at = created_at - interval '1 minute' where product_id = ?", active);
        assertThat(wishlist.add(customer, second).productIds()).containsExactly(second, active);
        assertThat(wishlist.remove(customer, active).productIds()).containsExactly(second);
    }

    @Test
    void stoppedProductsCannotBeAddedAndAreHidden() {
        assertThatThrownBy(() -> wishlist.add(customer, stopped)).isInstanceOf(BusinessException.class);
        wishlist.add(customer, active);
        jdbc.update("update products set status = 'INACTIVE' where id = ?", active);
        assertThat(wishlist.view(customer).productIds()).isEmpty();
        jdbc.update("update products set status = 'ACTIVE' where id = ?", active);
        assertThat(wishlist.view(customer).productIds()).containsExactly(active);
    }

    @Test
    void mergeSkipsUnknownStoppedAndDuplicates() {
        wishlist.add(customer, active);
        var merged = wishlist.merge(customer, new WishlistDtos.GuestWishlist(
                List.of(second, second, stopped, UUID.randomUUID(), active)));
        assertThat(merged.productIds()).containsExactlyInAnyOrder(active, second);
    }

    @Test
    void limitIsEnforced() {
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < WishlistDtos.MAX_ITEMS; i++) {
            UUID id = UUID.randomUUID();
            product(id, "ZQXW-L" + i, "ACTIVE");
            rows.add(new Object[]{customer, id});
        }
        jdbc.batchUpdate("insert into wishlist_items(customer_id, product_id) values (?, ?)", rows);
        assertThatThrownBy(() -> wishlist.add(customer, active)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("tối đa");
        assertThat(wishlist.merge(customer, new WishlistDtos.GuestWishlist(List.of(active))).productIds())
                .hasSize(WishlistDtos.MAX_ITEMS).doesNotContain(active);
    }
}
