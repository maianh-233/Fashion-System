package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fashionsystem.fashion_system.helper.MailHelper;
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

/** Nhắc giỏ hàng trên cơ sở dữ liệu thật; dữ liệu do test tạo và rollback sau mỗi test. */
@SpringBootTest
@Transactional
class CartReminderIntegrationTest {
    @Autowired CartReminderService reminders;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean NotificationPublisher notifications;
    @MockitoBean MailHelper mail;

    final UUID customer = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID shirt = UUID.randomUUID();
    final UUID other = UUID.randomUUID();
    String email;

    @BeforeEach
    void setUp() {
        String tag = customer.toString().substring(0, 8);
        email = "zqcr-" + tag + "@example.com";
        jdbc.update("insert into customers(id, customer_code, membership_status, source, full_name, email) values (?, ?, 'MEMBER', 'WEBSITE', 'Zqcr Khách', ?)",
                customer, "ZQCR-" + tag, email);
        jdbc.update("insert into products(id, code, name, slug, status, image_url) values (?, ?, 'Zqcr Sơ mi', ?, 'ACTIVE', '')",
                product, "ZQCR-" + tag, "zqcr-so-mi-" + tag);
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Trắng', 'M', 500000, true)",
                shirt, product, "ZQCR-" + tag + "-M");
        jdbc.update("insert into product_variants(id, product_id, sku, color, size, price, active) values (?, ?, ?, 'Trắng', 'L', 500000, true)",
                other, product, "ZQCR-" + tag + "-L");
    }

    @Test
    void remindsOnceAfterTheCartIsIdleAndAgainOnlyAfterItChanges() {
        cartLine(shirt, 2, "25 hours");
        cartLine(other, 1, "26 hours");
        assertThat(remindedNow()).isTrue();
        assertThat(remindedNow()).isFalse();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        // Gửi trên luồng nền: chờ thông báo của khách test (khách có sẵn trong DB cũng có thể được gửi cùng lúc).
        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notifications, org.mockito.Mockito.timeout(5000)).publish(org.mockito.ArgumentMatchers.argThat(
                n -> n.recipientIdentities().contains(RealtimeIdentity.customer(customer))));
        verify(notifications, org.mockito.Mockito.atLeastOnce()).publish(sent.capture());
        assertThat(sent.getAllValues()).filteredOn(n -> n.recipientIdentities().contains(RealtimeIdentity.customer(customer)))
                .singleElement().satisfies(n -> {
                    assertThat(n.type()).isEqualTo(NotificationType.CART_REMINDER);
                    assertThat(n.message()).startsWith("Bạn còn 3 sản phẩm trong giỏ: Zqcr Sơ mi và 1 mẫu khác.");
                    assertThat(n.actionUrl()).isEqualTo("/carts");
                });
        verify(mail, org.mockito.Mockito.timeout(5000)).sendText(eq(email), anyString(), contains("Zqcr Sơ mi"));

        // Khách sửa giỏ rồi lại bỏ đó: nhắc thêm một lần.
        jdbc.update("update cart_items set updated_at = now() at time zone 'UTC' - interval '24 hours 30 minutes' where product_variant_id = ?", shirt);
        assertThat(remindedNow()).isTrue();
    }

    @Test
    void skipsFreshCartsOldCartsAndCustomersWhoOrderedSince() {
        cartLine(shirt, 1, "2 hours");
        assertThat(remindedNow()).isFalse();

        jdbc.update("update cart_items set updated_at = now() at time zone 'UTC' - interval '8 days' where customer_id = ?", customer);
        assertThat(remindedNow()).isFalse();

        jdbc.update("update cart_items set updated_at = now() at time zone 'UTC' - interval '30 hours' where customer_id = ?", customer);
        jdbc.update("insert into orders(id, order_code, order_type, status, customer_id, created_at) values (?, ?, 'ONLINE', 'PENDING', ?, now() at time zone 'UTC' - interval '1 hour')",
                UUID.randomUUID(), "ZQCR-" + customer.toString().substring(0, 8), customer);
        assertThat(remindedNow()).isFalse();
    }

    @Test
    void cartsWithOnlyDiscontinuedProductsAreNotReminded() {
        cartLine(shirt, 1, "30 hours");
        jdbc.update("update products set status = 'INACTIVE' where id = ?", product);
        assertThat(remindedNow()).isFalse();
    }

    @Test
    void summaryCountsUnitsAndOtherModels() {
        assertThat(CartReminderService.summary(1, 1, "Áo")).isEqualTo("Bạn còn 1 sản phẩm trong giỏ: Áo.");
        assertThat(CartReminderService.summary(3, 1, "Áo")).isEqualTo("Bạn còn 3 sản phẩm trong giỏ: Áo.");
        assertThat(CartReminderService.summary(4, 3, "Áo")).isEqualTo("Bạn còn 4 sản phẩm trong giỏ: Áo và 2 mẫu khác.");
    }

    /** Có nhắc khách của test trong lần chạy này không (giỏ có sẵn trong DB có thể được nhắc cùng lúc). */
    private boolean remindedNow() {
        String before = remindedCartStamp();
        reminders.remindIdleCarts();
        String after = remindedCartStamp();
        return after != null && !after.equals(before);
    }

    private String remindedCartStamp() {
        return jdbc.queryForList("select cart_updated_at::text from cart_reminders where customer_id = ?", String.class, customer)
                .stream().findFirst().orElse(null);
    }

    private void cartLine(UUID variant, int quantity, String age) {
        jdbc.update("insert into cart_items(customer_id, product_variant_id, quantity, created_at, updated_at) values (?, ?, ?, "
                + "now() at time zone 'UTC' - interval '" + age + "', now() at time zone 'UTC' - interval '" + age + "')",
                customer, variant, quantity);
    }
}
