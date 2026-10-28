package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fashionsystem.fashion_system.dto.storefront.StockAlertDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** "Báo khi có hàng" trên cơ sở dữ liệu thật; dữ liệu do test tạo và rollback sau mỗi test. */
@SpringBootTest
@Transactional
class StockAlertIntegrationTest {
    @Autowired StockAlertService alerts;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean NotificationPublisher notifications;

    final UUID store = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID soldOut = UUID.randomUUID();
    final UUID inStock = UUID.randomUUID();
    final UUID customer = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        String tag = product.toString().substring(0, 8);
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, 'Zqsa Store', true, 10.7512345, 106.6712345)",
                store, "ZQSA-" + tag);
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, ?, 'Zqsa Áo khoác', ?, 'ACTIVE', '')",
                product, "ZQSA-" + tag, "zqsa-ao-khoac-" + tag);
        variant(soldOut, "ZQSA-" + tag + "-M", "M", 0);
        variant(inStock, "ZQSA-" + tag + "-L", "L", 4);
        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name) values (?, ?, 'MEMBER', 'WEBSITE', 'Zqsa Khách')",
                customer, "ZQSA-" + tag);
    }

    @Test
    void subscribesOnlyToSoldOutVariantsAndIsIdempotent() {
        assertThat(alerts.subscribe(customer, soldOut).variantIds()).containsExactly(soldOut);
        assertThat(alerts.subscribe(customer, soldOut).variantIds()).containsExactly(soldOut);
        assertThat(pendingCount()).isEqualTo(1);

        assertThatThrownBy(() -> alerts.subscribe(customer, inStock))
                .isInstanceOf(BusinessException.class).hasMessageContaining("còn hàng");

        assertThat(alerts.mine(customer)).singleElement().satisfies(alert -> {
            assertThat(alert.variantId()).isEqualTo(soldOut);
            assertThat(alert.size()).isEqualTo("M");
            assertThat(alert.inStock()).isFalse();
            assertThat(alert.notifiedAt()).isNull();
        });

        assertThat(alerts.unsubscribe(customer, soldOut).variantIds()).isEmpty();
        assertThat(pendingCount()).isZero();
    }

    @Test
    void notifiesOnceWhenOnlineStockReturns() {
        alerts.subscribe(customer, soldOut);
        assertThat(alerts.notifyRestocked()).isZero();

        // Hàng chỉ ở kênh tại quầy thì chưa tính là có hàng online.
        jdbc.update("update inventory_balances set available_quantity = 3 where store_id = ? and product_variant_id = ?", store, soldOut);
        assertThat(alerts.notifyRestocked()).isZero();

        jdbc.update("update inventory_balances set online_quantity = 2 where store_id = ? and product_variant_id = ?", store, soldOut);
        assertThat(alerts.notifyRestocked()).isEqualTo(1);
        verifyNoInteractions(notifications);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notifications).publish(sent.capture());
        assertThat(sent.getValue().type()).isEqualTo(NotificationType.BACK_IN_STOCK);
        assertThat(sent.getValue().recipientIdentities()).containsExactly(RealtimeIdentity.customer(customer));
        assertThat(sent.getValue().message()).contains("Zqsa Áo khoác (Đen / M)");
        assertThat(sent.getValue().actionUrl()).isEqualTo("/productdetail?id=" + product);

        assertThat(pendingCount()).isZero();
        assertThat(alerts.notifyRestocked()).isZero();
        assertThat(alerts.mine(customer)).singleElement().extracting(StockAlertDtos.Alert::notifiedAt).isNotNull();
    }

    @Test
    void stoppedProductsAndInactiveStoresDoNotTriggerAlerts() {
        alerts.subscribe(customer, soldOut);
        jdbc.update("update inventory_balances set online_quantity = 2 where store_id = ? and product_variant_id = ?", store, soldOut);
        jdbc.update("update stores set active = false where id = ?", store);
        assertThat(alerts.notifyRestocked()).isZero();

        jdbc.update("update stores set active = true where id = ?", store);
        jdbc.update("update product_variants set active = false where id = ?", soldOut);
        assertThat(alerts.notifyRestocked()).isZero();
        assertThat(pendingCount()).isEqualTo(1);
    }

    private void variant(UUID id, String sku, String size, int onlineStock) {
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Đen', ?, 900000, true)",
                id, product, sku, size);
        jdbc.update("insert into inventory_balances(store_id, product_variant_id, available_quantity, online_quantity, version) values (?, ?, 0, ?, 0)",
                store, id, onlineStock);
    }

    private int pendingCount() {
        return jdbc.queryForObject("select count(*) from stock_alerts where customer_id = ? and notified_at is null", Integer.class, customer);
    }
}
