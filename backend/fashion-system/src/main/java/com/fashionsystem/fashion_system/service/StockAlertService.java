package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.storefront.StockAlertDtos;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * "Báo khi có hàng" (bảng {@code stock_alerts}). Còn hàng nghĩa là tổng tồn online tại cửa hàng đang hoạt động
 * có tọa độ lớn hơn 0, giống cách trang sản phẩm tính "available". {@link StockAlertScheduler} gọi
 * {@link #notifyRestocked()} định kỳ; mỗi đăng ký chỉ được báo một lần.
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Back-in-stock subscriptions are customer working state; they never change orders, stock or payments")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAlertService {
    /** Tồn online của biến thể {@code v} tại cửa hàng bán online. */
    private static final String ONLINE_STOCK = """
            coalesce((select sum(b.online_quantity) from inventory_balances b join stores s on s.id = b.store_id
                      where b.product_variant_id = v.id and b.online_quantity > 0 and s.active = true
                        and s.latitude is not null and s.longitude is not null), 0)""";

    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationPublisher notifications;

    @Transactional
    public StockAlertDtos.Pending subscribe(UUID customerId, UUID variantId) {
        Map<String, Object> variant = jdbc.queryForList("""
                select p.id as product_id, (v.active and p.status = 'ACTIVE') as sellable,
                       """ + " " + ONLINE_STOCK + """
                 as stock
                from product_variants v join products p on p.id = v.product_id where v.id = :variantId""",
                new MapSqlParameterSource("variantId", variantId)).stream().findFirst()
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm không tồn tại"));
        if (!Boolean.TRUE.equals(variant.get("sellable"))) throw BusinessException.badRequest("Sản phẩm đã ngừng bán");
        if (((Number) variant.get("stock")).longValue() > 0)
            throw BusinessException.badRequest("Sản phẩm đang còn hàng, bạn có thể đặt ngay");
        MapSqlParameterSource params = new MapSqlParameterSource("customerId", customerId).addValue("variantId", variantId);
        Integer pending = jdbc.queryForObject(
                "select count(*) from stock_alerts where customer_id = :customerId and notified_at is null", params, Integer.class);
        boolean already = Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from stock_alerts where customer_id = :customerId
                               and product_variant_id = :variantId and notified_at is null)""", params, Boolean.class));
        if (!already && pending != null && pending >= StockAlertDtos.MAX_PENDING)
            throw BusinessException.conflict("Bạn chỉ có thể chờ tối đa " + StockAlertDtos.MAX_PENDING + " sản phẩm cùng lúc");
        jdbc.update("""
                insert into stock_alerts (customer_id, product_variant_id) values (:customerId, :variantId)
                on conflict (customer_id, product_variant_id) where notified_at is null do nothing""", params);
        return pending(customerId, (UUID) variant.get("product_id"));
    }

    @Transactional
    public StockAlertDtos.Pending unsubscribe(UUID customerId, UUID variantId) {
        UUID productId = jdbc.queryForList("select product_id from product_variants where id = :variantId",
                new MapSqlParameterSource("variantId", variantId), UUID.class).stream().findFirst().orElse(null);
        jdbc.update("delete from stock_alerts where customer_id = :customerId and product_variant_id = :variantId and notified_at is null",
                new MapSqlParameterSource("customerId", customerId).addValue("variantId", variantId));
        return pending(customerId, productId);
    }

    /** Biến thể của sản phẩm mà khách đang chờ có hàng. */
    public StockAlertDtos.Pending pending(UUID customerId, UUID productId) {
        if (productId == null) return new StockAlertDtos.Pending(List.of());
        return new StockAlertDtos.Pending(jdbc.queryForList("""
                select a.product_variant_id from stock_alerts a join product_variants v on v.id = a.product_variant_id
                where a.customer_id = :customerId and v.product_id = :productId and a.notified_at is null""",
                new MapSqlParameterSource("customerId", customerId).addValue("productId", productId), UUID.class));
    }

    /** Đăng ký của khách: đang chờ trước, rồi đã được báo (mới nhất trước), tối đa 100 dòng. */
    public List<StockAlertDtos.Alert> mine(UUID customerId) {
        return jdbc.query("""
                select a.id, p.id as product_id, v.id as variant_id, p.name, v.color, v.size, a.created_at, a.notified_at,
                       coalesce(nullif(p.image_url, ''), (select i.image_url from product_images i
                           join product_variants iv on iv.id = i.product_variant_id where iv.product_id = p.id
                           order by i.is_primary desc nulls last, i.sort_order nulls last limit 1)) as image_url,
                       """ + " " + ONLINE_STOCK + """
                 as stock
                from stock_alerts a
                join product_variants v on v.id = a.product_variant_id
                join products p on p.id = v.product_id
                where a.customer_id = :customerId
                order by (a.notified_at is null) desc, coalesce(a.notified_at, a.created_at) desc
                limit 100""", new MapSqlParameterSource("customerId", customerId), (rs, i) -> new StockAlertDtos.Alert(
                rs.getObject("id", UUID.class), rs.getObject("product_id", UUID.class), rs.getObject("variant_id", UUID.class),
                rs.getString("name"), rs.getString("color"), rs.getString("size"), rs.getString("image_url"),
                rs.getLong("stock") > 0, toLocal(rs.getTimestamp("created_at")), toLocal(rs.getTimestamp("notified_at"))));
    }

    /**
     * Đánh dấu mọi đăng ký đang chờ của biến thể đã có hàng online (và còn bán) là đã báo, rồi gửi thông báo cho khách
     * sau khi commit. Khóa {@code skip locked} để hai lần chạy song song không báo trùng.
     */
    @Transactional
    public int notifyRestocked() {
        List<StockAlertDtos.Restocked> restocked = jdbc.query("""
                with due as (
                    select a.id from stock_alerts a
                    join product_variants v on v.id = a.product_variant_id
                    join products p on p.id = v.product_id
                    where a.notified_at is null and v.active and p.status = 'ACTIVE' and """ + " " + ONLINE_STOCK + """
                 > 0
                    order by a.created_at
                    limit 500
                    for update of a skip locked
                )
                update stock_alerts a set notified_at = now() at time zone 'UTC'
                from due, product_variants v, products p
                where a.id = due.id and v.id = a.product_variant_id and p.id = v.product_id
                returning a.id, a.customer_id, p.id as product_id, p.name, v.color, v.size""",
                new MapSqlParameterSource(), (rs, i) -> new StockAlertDtos.Restocked(
                        rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                        rs.getObject("product_id", UUID.class), rs.getString("name"), rs.getString("color"), rs.getString("size")));
        if (restocked.isEmpty()) return 0;
        List<StockAlertDtos.Restocked> toSend = new ArrayList<>(restocked);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                toSend.forEach(StockAlertService.this::send);
            }
        });
        return restocked.size();
    }

    private void send(StockAlertDtos.Restocked alert) {
        String label = variantLabel(alert.color(), alert.size());
        try {
            notifications.publish(new NotificationRequest(List.of(RealtimeIdentity.customer(alert.customerId())), null,
                    NotificationType.BACK_IN_STOCK, "Sản phẩm bạn chờ đã có hàng",
                    alert.productName() + (label == null ? "" : " (" + label + ")") + " đã có hàng trở lại. Đặt ngay trước khi hết!",
                    "PRODUCT", alert.productId(), "/productdetail?id=" + alert.productId(),
                    Map.of("alertId", alert.alertId().toString())));
        } catch (RuntimeException failure) {
            log.warn("Back-in-stock notification for alert {} failed: {}", alert.alertId(), failure.getMessage());
        }
    }

    static String variantLabel(String color, String size) {
        List<String> parts = new ArrayList<>();
        if (color != null && !color.isBlank()) parts.add(color.trim());
        if (size != null && !size.isBlank()) parts.add(size.trim());
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private static LocalDateTime toLocal(Timestamp value) {
        return DbTimes.fromDb(value);
    }
}
