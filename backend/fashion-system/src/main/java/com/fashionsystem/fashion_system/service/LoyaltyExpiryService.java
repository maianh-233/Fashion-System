package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
 * Hết hạn điểm tích lũy và nhắc trước. {@link LoyaltyExpiryScheduler} gọi {@link #expireDue} mỗi ngày: phần còn lại
 * của các đợt đã tới hạn thành một giao dịch EXPIRE cho mỗi khách và trừ khỏi số dư. {@link #remindDue} nhắc khách
 * (thông báo + email) khi có điểm sẽ hết hạn trong {@value #FIRST_REMINDER_DAYS} và {@value #LAST_REMINDER_DAYS} ngày;
 * mỗi đợt được nhắc tối đa một lần ở mỗi mốc.
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "Point expiry is a scheduled ledger job; every change is an EXPIRE row in loyalty_transactions")
@Transactional(readOnly = true)
public class LoyaltyExpiryService {
    static final String EXPIRE = "EXPIRE";
    static final int FIRST_REMINDER_DAYS = 30;
    static final int LAST_REMINDER_DAYS = 7;
    private static final int BATCH = 500;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationPublisher notifications;
    private final MailHelper mail;
    /** Kafka/SMTP chậm không được giữ luồng scheduler dùng chung với các job khác. */
    private final TaskExecutor delivery;

    public LoyaltyExpiryService(NamedParameterJdbcTemplate jdbc, NotificationPublisher notifications, MailHelper mail,
            @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.mail = mail;
        this.delivery = delivery;
    }

    record Reminder(UUID customerId, String fullName, String email, int points, LocalDateTime expiresAt, int days) {
    }

    /** Đợt sắp hết hạn gần nhất của khách (cho ví điểm). */
    public record Upcoming(int points, LocalDateTime expiresAt) {
    }

    /** Các mốc hết hạn sắp tới của khách, gần nhất trước (gộp theo ngày, tối đa 3 mốc). */
    public List<Upcoming> upcoming(UUID customerId) {
        return jdbc.query("""
                select expires_at, sum(points_remaining) as points
                from loyalty_point_lots where customer_id = :customerId and points_remaining > 0
                  and expires_at > now() at time zone 'UTC'
                group by expires_at order by expires_at limit 3""", new MapSqlParameterSource("customerId", customerId),
                (rs, i) -> new Upcoming(rs.getInt("points"), time(rs.getTimestamp("expires_at"))));
    }

    /**
     * Cho hết hạn phần còn lại của các đợt đã tới hạn (tối đa {@value #BATCH} khách mỗi lượt).
     *
     * @return số khách bị trừ điểm hết hạn
     */
    @Transactional
    public int expireDue(LocalDateTime now) {
        MapSqlParameterSource at = new MapSqlParameterSource("now", DbTimes.toDb(now)).addValue("batch", BATCH);
        List<UUID> customers = jdbc.queryForList("""
                select distinct customer_id from loyalty_point_lots
                where points_remaining > 0 and expires_at <= :now limit :batch""", at, UUID.class);
        int expired = 0;
        for (UUID customerId : customers) {
            MapSqlParameterSource params = new MapSqlParameterSource("customerId", customerId).addValue("now", DbTimes.toDb(now));
            // Khóa ví trước rồi mới tới các đợt, cùng thứ tự với tích / đổi / hoàn điểm để không khóa chéo.
            Integer balance = jdbc.queryForList("select points_balance from loyalty_accounts where customer_id = :customerId for update",
                    params, Integer.class).stream().findFirst().orElse(null);
            Map<String, Object> lots = jdbc.queryForMap("""
                    with due as (
                        select id, points_remaining from loyalty_point_lots
                        where customer_id = :customerId and points_remaining > 0 and expires_at <= :now for update
                    ), done as (
                        update loyalty_point_lots l set points_expired = l.points_expired + due.points_remaining,
                               points_remaining = 0
                        from due where l.id = due.id
                        returning due.points_remaining as points, l.earned_at
                    )
                    select coalesce(sum(points), 0) as points, max(earned_at) as last_earned from done""", params);
            int points = ((Number) lots.get("points")).intValue();
            if (points <= 0) continue;
            int deducted = balance == null ? 0 : Math.min(points, balance);
            if (deducted > 0)
                jdbc.update("""
                        update loyalty_accounts set points_balance = points_balance - :points,
                               updated_at = now() at time zone 'UTC' where customer_id = :customerId""",
                        params.addValue("points", deducted));
            LocalDateTime lastEarned = DbTimes.fromDb(lots.get("last_earned"));
            jdbc.update("""
                    insert into loyalty_transactions (customer_id, transaction_type, points, note, created_at)
                    values (:customerId, 'EXPIRE', :points, :note, now() at time zone 'UTC')""",
                    new MapSqlParameterSource("customerId", customerId).addValue("points", -deducted)
                            .addValue("note", expireNote(points, lastEarned, deducted)));
            expired++;
        }
        return expired;
    }

    /**
     * Nhắc khách có điểm sắp hết hạn: mốc {@value #LAST_REMINDER_DAYS} ngày trước (đánh dấu luôn mốc
     * {@value #FIRST_REMINDER_DAYS} ngày để không nhắc hai lần liền nhau), rồi mốc {@value #FIRST_REMINDER_DAYS} ngày.
     *
     * @return số lời nhắc đã hẹn gửi
     */
    @Transactional
    public int remindDue(LocalDateTime now) {
        List<Reminder> reminders = new java.util.ArrayList<>(due(now, LAST_REMINDER_DAYS,
                "reminded_7_at = :now, reminded_30_at = coalesce(l.reminded_30_at, :now)", "reminded_7_at"));
        reminders.addAll(due(now, FIRST_REMINDER_DAYS, "reminded_30_at = :now", "reminded_30_at"));
        if (reminders.isEmpty()) return 0;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                delivery.execute(() -> reminders.forEach(LoyaltyExpiryService.this::send));
            }
        });
        return reminders.size();
    }

    private List<Reminder> due(LocalDateTime now, int days, String mark, String markColumn) {
        return jdbc.query("""
                with due as (
                    select id from loyalty_point_lots
                    where points_remaining > 0 and expires_at > :now and expires_at <= :until and %s is null
                    for update skip locked
                ), marked as (
                    update loyalty_point_lots l set %s from due where l.id = due.id
                    returning l.customer_id, l.points_remaining, l.expires_at
                )
                select m.customer_id, c.full_name, c.email, sum(m.points_remaining) as points, min(m.expires_at) as expires_at
                from marked m join customers c on c.id = m.customer_id and c.active
                group by m.customer_id, c.full_name, c.email""".formatted(markColumn, mark),
                new MapSqlParameterSource("now", DbTimes.toDb(now)).addValue("until", DbTimes.toDb(now.plusDays(days))),
                (rs, i) -> new Reminder(rs.getObject("customer_id", UUID.class), rs.getString("full_name"),
                        rs.getString("email"), rs.getInt("points"), time(rs.getTimestamp("expires_at")), days));
    }

    private void send(Reminder reminder) {
        String message = reminderMessage(reminder.points(), reminder.expiresAt());
        try {
            notifications.publish(new NotificationRequest(List.of(RealtimeIdentity.customer(reminder.customerId())), null,
                    NotificationType.POINTS_EXPIRING, "Điểm tích lũy sắp hết hạn", message, "LOYALTY", null,
                    "/vouchers", Map.of("points", reminder.points(), "days", reminder.days())));
        } catch (RuntimeException failure) {
            log.warn("Points expiry reminder for {} failed: {}", reminder.customerId(), failure.getMessage());
        }
        if (!StringUtils.hasText(reminder.email())) return;
        try {
            String name = StringUtils.hasText(reminder.fullName()) ? reminder.fullName().trim() : "bạn";
            mail.sendText(reminder.email(), "Điểm tích lũy của bạn sắp hết hạn - Lunaria",
                    "Chào " + name + ",\n\n" + message
                            + "\n\nĐăng nhập Lunaria, mở Ví voucher để đổi điểm lấy voucher và dùng khi mua online"
                            + " hoặc tại cửa hàng.\n\n— Lunaria Boutique");
        } catch (RuntimeException failure) {
            log.warn("Points expiry email for {} failed: {}", reminder.customerId(), failure.getMessage());
        }
    }

    /** Ngày cuối dùng được là ngày trước thời điểm hết hạn khi hết hạn đúng nửa đêm. */
    static String lastDay(LocalDateTime expiresAt) {
        return expiresAt.minusSeconds(1).format(DATE);
    }

    static String reminderMessage(int points, LocalDateTime expiresAt) {
        return "Bạn có " + count(points) + " điểm sẽ hết hạn sau ngày " + lastDay(expiresAt) + ". Đổi điểm lấy voucher trước khi hết hạn nhé!";
    }

    static String expireNote(int points, LocalDateTime lastEarned, int deducted) {
        String note = "Hết hạn " + count(points) + " điểm" + (lastEarned == null ? "" : " tích đến ngày " + lastEarned.format(DATE));
        return deducted < points ? note + " (số dư chỉ còn " + count(deducted) + " điểm)" : note;
    }

    private static String count(int points) {
        return java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(points);
    }

    private static LocalDateTime time(Timestamp value) {
        return DbTimes.fromDb(value);
    }
}
