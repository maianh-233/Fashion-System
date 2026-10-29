package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.dto.FlashSaleDtos;
import com.fashionsystem.fashion_system.dto.order.OfflineCartRequest;
import com.fashionsystem.fashion_system.dto.order.OnlineCheckoutRequest;
import com.fashionsystem.fashion_system.dto.order.OnlineOrderPlacedResponse;
import com.fashionsystem.fashion_system.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Flash sale với luồng đặt đơn online thật; dữ liệu do test tạo và rollback sau mỗi test. */
@SpringBootTest
@Transactional
class FlashSaleIntegrationTest {
    @Autowired FlashSaleService flashSales;
    @Autowired OnlineOrderService onlineOrders;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    final UUID actor = UUID.randomUUID();
    final UUID store = UUID.randomUUID();
    final UUID variant = UUID.randomUUID();
    final UUID other = UUID.randomUUID();
    final UUID alice = UUID.randomUUID();
    final UUID bob = UUID.randomUUID();
    UUID aliceAddress;
    UUID bobAddress;

    @BeforeEach
    void setUp() {
        String tag = store.toString().substring(0, 8);
        jdbc.update("insert into users(id, username) values (?, ?)", actor, "zqfs-" + tag);
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, 'Zqfs Store', true, 10.7612345, 106.6812345)",
                store, "ZQFS-" + tag);
        UUID product = UUID.randomUUID();
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, ?, 'Zqfs Váy', ?, 'ACTIVE', '')",
                product, "ZQFS-" + tag, "zqfs-vay-" + tag);
        for (Object[] v : new Object[][] {{variant, "S"}, {other, "M"}}) {
            jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Đỏ', ?, 400000, true)",
                    v[0], product, "ZQFS-" + tag + "-" + v[1], v[1]);
            jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, 0, 50, 0)",
                    store, v[0]);
        }
        aliceAddress = customer(alice, tag + "a");
        bobAddress = customer(bob, tag + "b");
    }

    @Test
    void flashPriceIsLimitedToTwoPerCustomerAndTheQuota() {
        FlashSaleDtos.AdminSale sale = createSale(3);
        assertThat(sale.status()).isEqualTo("RUNNING");
        assertThat(flashSales.running()).anySatisfy(live -> {
            assertThat(live.id()).isEqualTo(sale.id());
            assertThat(live.items()).singleElement().satisfies(item -> assertThat(item.remaining()).isEqualTo(3));
        });

        // Alice mua 3: 2 món giá flash (250.000), 1 món giá thường.
        OnlineOrderPlacedResponse first = place(alice, aliceAddress, 3, "a1");
        assertThat(first.total()).isEqualByComparingTo(new BigDecimal("900000"));
        assertThat(jdbc.queryForObject("select sum(discount_amount) from order_items where order_id = ?", BigDecimal.class, first.id()))
                .isEqualByComparingTo("300000");
        assertThat(jdbc.queryForObject("select promotion_code from order_items where order_id = ?", String.class, first.id()))
                .isEqualTo(FlashSaleDtos.LINE_LABEL);

        // Alice đã dùng hết 2 món của mình: lần sau giá thường.
        assertThat(place(alice, aliceAddress, 1, "a2").total()).isEqualByComparingTo("400000");
        // Bob chỉ còn 1 suất.
        assertThat(place(bob, bobAddress, 2, "b1").total()).isEqualByComparingTo("650000");
        assertThat(remaining(sale.id())).isZero();
    }

    @Test
    void cancelledOrdersGiveTheirQuotaBack() {
        FlashSaleDtos.AdminSale sale = createSale(2);
        OnlineOrderPlacedResponse order = place(alice, aliceAddress, 2, "c1");
        assertThat(remaining(sale.id())).isZero();

        jdbc.update("update orders set status = 'CANCELLED' where id = ?", order.id());
        entityManager.clear();

        assertThat(remaining(sale.id())).isEqualTo(2);
        assertThat(place(bob, bobAddress, 1, "c2").total()).isEqualByComparingTo("250000");
    }

    @Test
    void validatesPriceWindowAndOverlap() {
        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(() -> asJob(() -> flashSales.create(actor, new FlashSaleDtos.SaleRequest("Sai giá", now, now.plusHours(1),
                List.of(new FlashSaleDtos.ItemRequest(variant, new BigDecimal("400000"), 5))))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("thấp hơn giá bán");
        assertThatThrownBy(() -> asJob(() -> flashSales.create(actor, new FlashSaleDtos.SaleRequest("Ngược giờ", now, now.minusHours(1),
                List.of(new FlashSaleDtos.ItemRequest(variant, new BigDecimal("1000"), 5))))))
                .hasMessageContaining("sau giờ bắt đầu");

        FlashSaleDtos.AdminSale sale = createSale(5);
        assertThatThrownBy(() -> asJob(() -> flashSales.create(actor, new FlashSaleDtos.SaleRequest("Chồng giờ", now.plusMinutes(30),
                now.plusHours(3), List.of(new FlashSaleDtos.ItemRequest(variant, new BigDecimal("200000"), 5))))))
                .hasMessageContaining("flash sale khác");

        assertThat(asJob(() -> flashSales.setActive(sale.id(), false)).status()).isEqualTo("STOPPED");
        assertThat(flashSales.running()).noneMatch(live -> live.id().equals(sale.id()));
        assertThat(place(alice, aliceAddress, 1, "d1").total()).isEqualByComparingTo("400000");
    }

    private FlashSaleDtos.AdminSale createSale(int quota) {
        LocalDateTime now = LocalDateTime.now();
        return asJob(() -> flashSales.create(actor, new FlashSaleDtos.SaleRequest("Zqfs giờ vàng", now.minusMinutes(5), now.plusHours(2),
                List.of(new FlashSaleDtos.ItemRequest(variant, new BigDecimal("250000"), quota)))));
    }

    private int remaining(UUID saleId) {
        return flashSales.running().stream().filter(live -> live.id().equals(saleId)).findFirst().orElseThrow()
                .items().getFirst().remaining();
    }

    private OnlineOrderPlacedResponse place(UUID customer, UUID address, int quantity, String requestId) {
        OnlineOrderPlacedResponse placed;
        try (var ignored = BusinessAuditContextFactory.systemJob("CUSTOMER_ORDER:" + customer)) {
            placed = onlineOrders.place(customer, new OnlineCheckoutRequest(address, List.of(new OfflineCartRequest.Line(variant, quantity)),
                    List.of(), "COD", null, requestId + "-" + customer));
        }
        entityManager.flush();
        entityManager.clear();
        return placed;
    }

    private <T> T asJob(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_FLASH_SALE")) {
            return action.get();
        }
    }

    private UUID customer(UUID id, String tag) {
        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name) values (?, ?, 'MEMBER', 'WEBSITE', 'Zqfs Khách')",
                id, "ZQFS-" + tag);
        UUID address = UUID.randomUUID();
        jdbc.update("insert into customer_addresses(id, customer_id, receiver_name, receiver_phone, address_line, latitude, longitude, "
                + "is_default, address_type, management_source) values (?, ?, 'Zqfs', '0900000001', '2 Lê Lợi', 10.7620000, 106.6820000, "
                + "true, 'HOME', 'WEB')", address, id);
        return address;
    }
}
