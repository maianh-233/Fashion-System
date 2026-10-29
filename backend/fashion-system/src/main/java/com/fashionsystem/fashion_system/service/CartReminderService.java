package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Nhắc giỏ hàng bị bỏ quên. Giỏ (chỉ tính sản phẩm còn bán) không thay đổi {@code cart-reminder.idle-hours} giờ và
 * khách chưa đặt đơn nào sau lần đổi giỏ cuối thì được nhắc một lần qua thông báo và email. Bảng
 * {@code cart_reminders} giữ lần đổi giỏ đã nhắc, nên chỉ nhắc lại khi giỏ có thay đổi mới hơn. Giỏ cũ hơn
 * {@code cart-reminder.max-age-days} ngày bị bỏ qua.
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Cart reminders are notification bookkeeping; they never change orders, stock or payments")
@Transactional(readOnly = true)
public class CartReminderService {
    /** Dòng giỏ còn bán được. */
    private static final String SELLABLE_LINES = """
            from cart_items ci join product_variants v on v.id = ci.product_variant_id
            join products p on p.id = v.product_id
            where v.active and p.status = 'ACTIVE'""";

    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationPublisher notifications;
    private final MailHelper mail;
    /** Kafka/SMTP chậm không được giữ luồng scheduler dùng chung với các job khác. */
    private final TaskExecutor delivery;

    public CartReminderService(NamedParameterJdbcTemplate jdbc, NotificationPublisher notifications, MailHelper mail,
            @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.mail = mail;
        this.delivery = delivery;
    }

    @Value("${cart-reminder.idle-hours:24}")
    private int idleHours = 24;

    @Value("${cart-reminder.max-age-days:7}")
    private int maxAgeDays = 7;

    record Reminder(UUID customerId, String fullName, String email, int itemCount, int lineCount, String firstProduct) {
    }

    /**
     * Đánh dấu và nhắc các giỏ đến hạn (tối đa 200 mỗi lần). Khóa theo {@code on conflict ... where} để hai lần chạy
     * song song không nhắc trùng.
     *
     * @return số khách được nhắc
     */
    @Transactional
    public int remindIdleCarts() {
        List<Reminder> due = jdbc.query("""
                with carts as (
                    select ci.customer_id, max(ci.updated_at) as cart_updated_at
                    """ + " " + SELLABLE_LINES + """

                    group by ci.customer_id
                ), due as (
                    select c.customer_id, c.cart_updated_at from carts c
                    join customers cu on cu.id = c.customer_id and cu.active
                    left join cart_reminders r on r.customer_id = c.customer_id
                    where c.cart_updated_at <= now() at time zone 'UTC' - make_interval(hours => :idleHours)
                      and c.cart_updated_at > now() at time zone 'UTC' - make_interval(days => :maxAgeDays)
                      and (r.customer_id is null or r.cart_updated_at < c.cart_updated_at)
                      and not exists (select 1 from orders o where o.customer_id = c.customer_id
                                      and o.created_at > c.cart_updated_at)
                    order by c.cart_updated_at
                    limit 200
                ), marked as (
                    insert into cart_reminders (customer_id, cart_updated_at, reminded_at)
                    select customer_id, cart_updated_at, now() at time zone 'UTC' from due
                    on conflict (customer_id) do update
                        set cart_updated_at = excluded.cart_updated_at, reminded_at = excluded.reminded_at
                        where cart_reminders.cart_updated_at < excluded.cart_updated_at
                    returning customer_id
                )
                select m.customer_id, cu.full_name, cu.email,
                       (select coalesce(sum(ci.quantity), 0) """ + " " + SELLABLE_LINES + """
                 and ci.customer_id = m.customer_id) as item_count,
                       (select count(*) """ + " " + SELLABLE_LINES + """
                 and ci.customer_id = m.customer_id) as line_count,
                       (select p.name """ + " " + SELLABLE_LINES + """
                 and ci.customer_id = m.customer_id order by ci.updated_at desc limit 1) as first_product
                from marked m join customers cu on cu.id = m.customer_id""",
                new MapSqlParameterSource("idleHours", idleHours).addValue("maxAgeDays", maxAgeDays),
                (rs, i) -> new Reminder(rs.getObject("customer_id", UUID.class), rs.getString("full_name"),
                        rs.getString("email"), rs.getInt("item_count"), rs.getInt("line_count"),
                        rs.getString("first_product")));
        if (due.isEmpty()) return 0;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                delivery.execute(() -> due.forEach(CartReminderService.this::send));
            }
        });
        return due.size();
    }

    private void send(Reminder reminder) {
        String message = summary(reminder.itemCount(), reminder.lineCount(), reminder.firstProduct())
                + " Hoàn tất đơn hàng trước khi sản phẩm hết hàng nhé!";
        try {
            notifications.publish(new NotificationRequest(List.of(RealtimeIdentity.customer(reminder.customerId())), null,
                    NotificationType.CART_REMINDER, "Giỏ hàng của bạn đang chờ", message, "CART", null, "/carts",
                    Map.of("itemCount", reminder.itemCount())));
        } catch (RuntimeException failure) {
            log.warn("Cart reminder notification for {} failed: {}", reminder.customerId(), failure.getMessage());
        }
        if (!StringUtils.hasText(reminder.email())) return;
        try {
            String name = StringUtils.hasText(reminder.fullName()) ? reminder.fullName().trim() : "bạn";
            mail.sendText(reminder.email(), "Bạn còn sản phẩm trong giỏ hàng - Lunaria",
                    "Chào " + name + ",\n\n" + message
                            + "\n\nĐăng nhập Lunaria và mở Giỏ hàng để thanh toán.\n\n— Lunaria Boutique");
        } catch (RuntimeException failure) {
            log.warn("Cart reminder email for {} failed: {}", reminder.customerId(), failure.getMessage());
        }
    }

    /** "Bạn còn 3 sản phẩm trong giỏ: Áo khoác Zqsa và 1 mẫu khác." ({@code lineCount} = số dòng giỏ). */
    static String summary(int itemCount, int lineCount, String firstProduct) {
        String text = "Bạn còn " + itemCount + " sản phẩm trong giỏ";
        if (!StringUtils.hasText(firstProduct)) return text + ".";
        text += ": " + firstProduct.trim();
        return lineCount > 1 ? text + " và " + (lineCount - 1) + " mẫu khác." : text + ".";
    }
}
