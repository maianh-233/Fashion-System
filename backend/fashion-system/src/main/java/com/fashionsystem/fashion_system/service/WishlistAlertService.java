package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
 * Báo khi sản phẩm yêu thích giảm giá, vào flash sale hoặc sắp hết hàng. {@link WishlistAlertScheduler} gọi
 * {@link #scan()} định kỳ: so trạng thái hiện tại của sản phẩm với mốc đã báo lưu trên {@code wishlist_items}, gửi
 * thông báo trong app ngay (một thông báo mỗi sản phẩm), ghi sự kiện để gộp email tối đa một lần mỗi 24 giờ.
 *
 * <ul>
 *   <li>Giảm giá: giá thấp nhất khách thấy (giá sale nếu có) của các biến thể còn hàng online giảm từ
 *   {@value #PRICE_DROP_PERCENT}% so với mốc. Mốc ghi lần quét đầu (không báo) và nâng lên khi giá tăng, nên giảm lại
 *   sau khi tăng vẫn được báo.</li>
 *   <li>Flash sale: một đợt đang mở bán, còn suất, có biến thể còn hàng; mỗi đợt báo một lần.</li>
 *   <li>Sắp hết hàng: tổng tồn online từ 1 tới {@value #LOW_STOCK_THRESHOLD}; báo lại sau khi tồn vượt ngưỡng.</li>
 * </ul>
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Wishlist alerts are notification bookkeeping; they never change orders, stock or payments")
@Transactional(readOnly = true)
public class WishlistAlertService {
    static final int LOW_STOCK_THRESHOLD = 5;
    static final int PRICE_DROP_PERCENT = 5;
    static final int EMAIL_INTERVAL_HOURS = 24;
    static final int KEEP_EVENTS_DAYS = 30;
    private static final DateTimeFormatter UNTIL = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    /** Tồn online theo biến thể tại cửa hàng bán online (giống trang sản phẩm). */
    private static final String STOCK_CTE = """
            stock as (
                select b.product_variant_id, sum(b.online_quantity) as qty
                from inventory_balances b join stores s on s.id = b.store_id
                where b.online_quantity > 0 and s.active = true and s.latitude is not null and s.longitude is not null
                group by b.product_variant_id
            )""";

    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationPublisher notifications;
    private final MailHelper mail;
    /** Kafka/SMTP chậm không được giữ luồng scheduler dùng chung với các job khác. */
    private final TaskExecutor delivery;

    public WishlistAlertService(NamedParameterJdbcTemplate jdbc, NotificationPublisher notifications, MailHelper mail,
            @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.mail = mail;
        this.delivery = delivery;
    }

    enum EventType { FLASH_SALE, PRICE_DROP, LOW_STOCK }

    /** Trạng thái hiện tại của một sản phẩm yêu thích và mốc đã báo. */
    record Watched(UUID customerId, UUID productId, String productName, BigDecimal baselinePrice,
                   UUID notifiedFlashSaleId, LocalDateTime lowStockNotifiedAt, BigDecimal price, long stock,
                   UUID flashSaleId, BigDecimal flashPrice, LocalDateTime flashEndsAt) {
    }

    record Event(EventType type, String message) {
    }

    /** Mốc mới cần lưu và các sự kiện cần báo. */
    record Decision(BigDecimal baselinePrice, UUID notifiedFlashSaleId, LocalDateTime lowStockNotifiedAt,
                    List<Event> events) {
        boolean changes(Watched w) {
            return !events.isEmpty() || !Objects.equals(baselinePrice, w.baselinePrice())
                    || !Objects.equals(notifiedFlashSaleId, w.notifiedFlashSaleId())
                    || !Objects.equals(lowStockNotifiedAt, w.lowStockNotifiedAt());
        }
    }

    record Alert(UUID customerId, UUID productId, List<Event> events) {
    }

    record Digest(UUID customerId, String fullName, String email, List<String> lines, boolean offers) {
    }

    /** Khách bật / tắt báo sản phẩm yêu thích. Bật lại thì ghi mốc giá mới để không báo những lần giảm đã qua. */
    @Transactional
    public boolean setEnabled(UUID customerId, boolean enabled) {
        MapSqlParameterSource params = new MapSqlParameterSource("customerId", customerId).addValue("enabled", enabled);
        boolean wasEnabled = enabled(customerId);
        jdbc.update("update customers set wishlist_alerts_enabled = :enabled where id = :customerId", params);
        if (enabled && !wasEnabled)
            jdbc.update("update wishlist_items set baseline_price = null where customer_id = :customerId", params);
        return enabled;
    }

    public boolean enabled(UUID customerId) {
        return Boolean.TRUE.equals(jdbc.queryForList("select wishlist_alerts_enabled from customers where id = :customerId",
                new MapSqlParameterSource("customerId", customerId), Boolean.class).stream().findFirst().orElse(true));
    }

    /**
     * Một lượt quét: cập nhật mốc, ghi sự kiện, gửi thông báo sau khi commit và gửi email gộp tới hạn. Khóa
     * advisory để hai lượt chạy song song (nhiều instance) không báo trùng.
     *
     * @return số sản phẩm được báo
     */
    @Transactional
    public int scan() {
        if (!Boolean.TRUE.equals(jdbc.getJdbcTemplate().queryForObject(
                "select pg_try_advisory_xact_lock(hashtext('wishlist-alerts'))", Boolean.class))) return 0;
        LocalDateTime now = LocalDateTime.now();
        List<Alert> alerts = new ArrayList<>();
        for (Watched watched : watched()) {
            Decision decision = decide(watched, now);
            if (!decision.changes(watched)) continue;
            jdbc.update("""
                    update wishlist_items set baseline_price = :baseline, notified_flash_sale_id = :flash,
                           low_stock_notified_at = :low
                    where customer_id = :customerId and product_id = :productId""",
                    new MapSqlParameterSource("baseline", decision.baselinePrice())
                            .addValue("flash", decision.notifiedFlashSaleId())
                            .addValue("low", DbTimes.toDb(decision.lowStockNotifiedAt()))
                            .addValue("customerId", watched.customerId()).addValue("productId", watched.productId()));
            if (decision.events().isEmpty()) continue;
            for (Event event : decision.events())
                jdbc.update("""
                        insert into wishlist_alert_events (customer_id, product_id, event_type, message)
                        values (:customerId, :productId, :type, :message)""",
                        new MapSqlParameterSource("customerId", watched.customerId())
                                .addValue("productId", watched.productId()).addValue("type", event.type().name())
                                .addValue("message", event.message()));
            alerts.add(new Alert(watched.customerId(), watched.productId(), decision.events()));
        }
        jdbc.update("delete from wishlist_alert_events where created_at < now() at time zone 'UTC' - make_interval(days => :days)",
                new MapSqlParameterSource("days", KEEP_EVENTS_DAYS));
        List<Digest> digests = dueDigests();
        if (!alerts.isEmpty() || !digests.isEmpty())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    delivery.execute(() -> {
                        alerts.forEach(WishlistAlertService.this::notify);
                        digests.forEach(WishlistAlertService.this::email);
                    });
                }
            });
        return alerts.size();
    }

    /** So trạng thái hiện tại với mốc đã báo; tách riêng để kiểm thử không cần DB. */
    static Decision decide(Watched w, LocalDateTime now) {
        List<Event> events = new ArrayList<>();
        UUID flash = w.notifiedFlashSaleId();
        if (w.flashSaleId() != null && !w.flashSaleId().equals(flash)) {
            flash = w.flashSaleId();
            events.add(new Event(EventType.FLASH_SALE, w.productName() + " đang flash sale chỉ " + vnd(w.flashPrice())
                    + (w.flashEndsAt() == null ? "" : ", đến " + UNTIL.format(w.flashEndsAt())) + ". Số lượng có hạn!"));
        }

        BigDecimal baseline = w.baselinePrice();
        if (w.price() != null) {
            if (baseline == null || w.price().compareTo(baseline) > 0) {
                baseline = w.price();
            } else if (w.price().multiply(BigDecimal.valueOf(100))
                    .compareTo(baseline.multiply(BigDecimal.valueOf(100 - PRICE_DROP_PERCENT))) <= 0) {
                int percent = baseline.subtract(w.price()).multiply(BigDecimal.valueOf(100))
                        .divide(baseline, 0, RoundingMode.HALF_UP).intValue();
                events.add(new Event(EventType.PRICE_DROP, w.productName() + " giảm còn " + vnd(w.price())
                        + " (trước " + vnd(baseline) + ", −" + percent + "%)."));
                baseline = w.price();
            }
        }

        LocalDateTime low = w.lowStockNotifiedAt();
        if (w.stock() > LOW_STOCK_THRESHOLD) {
            low = null;
        } else if (w.stock() > 0 && low == null) {
            low = now;
            events.add(new Event(EventType.LOW_STOCK, w.productName() + " chỉ còn " + w.stock() + " sản phẩm."));
        }
        return new Decision(baseline, flash, low, List.copyOf(events));
    }

    /** Sản phẩm yêu thích đang bán của khách còn hoạt động và đang bật báo. */
    private List<Watched> watched() {
        return jdbc.query("with " + STOCK_CTE + """
                , prod as (
                    select v.product_id, coalesce(sum(st.qty), 0) as stock,
                           min(coalesce(v.sale_price, v.price)) filter (where st.qty > 0) as price
                    from product_variants v left join stock st on st.product_variant_id = v.id
                    where v.active
                    group by v.product_id
                ), flash as (
                    select distinct on (v.product_id) v.product_id, fs.id as sale_id, fi.sale_price, fs.ends_at
                    from flash_sale_items fi
                    join flash_sales fs on fs.id = fi.flash_sale_id
                    join product_variants v on v.id = fi.product_variant_id and v.active
                    join stock st on st.product_variant_id = v.id
                    where fs.active and fs.starts_at <= now() at time zone 'UTC' and fs.ends_at > now() at time zone 'UTC'
                      and fi.quota > coalesce((select sum(u.quantity) from flash_sale_usages u
                                               where u.flash_sale_item_id = fi.id), 0)
                    order by v.product_id, fi.sale_price, fs.ends_at
                )
                select w.customer_id, w.product_id, p.name, w.baseline_price, w.notified_flash_sale_id,
                       w.low_stock_notified_at, pr.price, pr.stock, f.sale_id, f.sale_price, f.ends_at
                from wishlist_items w
                join customers c on c.id = w.customer_id and c.active and c.wishlist_alerts_enabled
                join products p on p.id = w.product_id and p.status = 'ACTIVE'
                join prod pr on pr.product_id = w.product_id
                left join flash f on f.product_id = w.product_id""", new MapSqlParameterSource(),
                (rs, i) -> new Watched(rs.getObject("customer_id", UUID.class), rs.getObject("product_id", UUID.class),
                        rs.getString("name"), rs.getBigDecimal("baseline_price"),
                        rs.getObject("notified_flash_sale_id", UUID.class), time(rs.getTimestamp("low_stock_notified_at")),
                        rs.getBigDecimal("price"), rs.getLong("stock"), rs.getObject("sale_id", UUID.class),
                        rs.getBigDecimal("sale_price"), time(rs.getTimestamp("ends_at"))));
    }

    /**
     * Khách có sự kiện chưa gửi email và chưa nhận email nào trong {@value #EMAIL_INTERVAL_HOURS} giờ: đánh dấu đã gửi
     * mọi sự kiện đang chờ của họ và trả nội dung email (tối đa 100 khách mỗi lượt).
     */
    private List<Digest> dueDigests() {
        Map<UUID, Digest> digests = new LinkedHashMap<>();
        jdbc.query("""
                with due as (
                    select e.customer_id from wishlist_alert_events e
                    join customers c on c.id = e.customer_id and c.active and c.wishlist_alerts_enabled
                    where e.emailed_at is null and coalesce(trim(c.email), '') <> ''
                      and not exists (select 1 from wishlist_alert_events x where x.customer_id = e.customer_id
                                      and x.emailed_at > now() at time zone 'UTC' - make_interval(hours => :hours))
                    group by e.customer_id
                    limit 100
                ), marked as (
                    update wishlist_alert_events e set emailed_at = now() at time zone 'UTC'
                    from due where e.customer_id = due.customer_id and e.emailed_at is null
                    returning e.customer_id, e.event_type, e.message, e.created_at
                )
                select m.customer_id, c.full_name, c.email, m.event_type, m.message
                from marked m join customers c on c.id = m.customer_id
                order by m.customer_id, m.created_at""",
                new MapSqlParameterSource("hours", EMAIL_INTERVAL_HOURS), rs -> {
                    UUID customerId = rs.getObject("customer_id", UUID.class);
                    Digest current = digests.get(customerId);
                    List<String> lines = current == null ? new ArrayList<>() : current.lines();
                    lines.add(rs.getString("message"));
                    boolean offer = !EventType.LOW_STOCK.name().equals(rs.getString("event_type"));
                    digests.put(customerId, new Digest(customerId, rs.getString("full_name"), rs.getString("email"), lines,
                            offer || (current != null && current.offers())));
                });
        return List.copyOf(digests.values());
    }

    private void notify(Alert alert) {
        try {
            notifications.publish(new NotificationRequest(List.of(RealtimeIdentity.customer(alert.customerId())), null,
                    NotificationType.WISHLIST_ALERT, title(alert.events().getFirst().type()),
                    String.join(" ", alert.events().stream().map(Event::message).toList()),
                    "PRODUCT", alert.productId(), "/productdetail?id=" + alert.productId(),
                    Map.of("events", alert.events().stream().map(e -> e.type().name()).toList())));
        } catch (RuntimeException failure) {
            log.warn("Wishlist alert notification for {} failed: {}", alert.customerId(), failure.getMessage());
        }
    }

    private void email(Digest digest) {
        try {
            String name = StringUtils.hasText(digest.fullName()) ? digest.fullName().trim() : "bạn";
            mail.sendText(digest.email(), subject(digest.offers()), body(name, digest.lines()));
        } catch (RuntimeException failure) {
            log.warn("Wishlist alert email for {} failed: {}", digest.customerId(), failure.getMessage());
        }
    }

    static String title(EventType type) {
        return switch (type) {
            case FLASH_SALE -> "Sản phẩm yêu thích đang flash sale";
            case PRICE_DROP -> "Sản phẩm yêu thích vừa giảm giá";
            case LOW_STOCK -> "Sản phẩm yêu thích sắp hết hàng";
        };
    }

    static String subject(boolean offers) {
        return offers ? "Sản phẩm bạn yêu thích đang có giá tốt - Lunaria" : "Sản phẩm bạn yêu thích sắp hết hàng - Lunaria";
    }

    static String body(String name, List<String> lines) {
        StringBuilder text = new StringBuilder("Chào ").append(name).append(",\n\nCập nhật về sản phẩm trong danh sách yêu thích của bạn:\n\n");
        lines.forEach(line -> text.append("• ").append(line).append('\n'));
        return text.append("\nĐăng nhập Lunaria và mở mục Yêu thích để xem chi tiết.\n")
                .append("Không muốn nhận email này? Tắt \"Báo sản phẩm yêu thích\" trong trang Hồ sơ.\n\n— Lunaria Boutique")
                .toString();
    }

    static String vnd(BigDecimal amount) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + "đ";
    }

    private static LocalDateTime time(Timestamp value) {
        return DbTimes.fromDb(value);
    }
}
