package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
 * Voucher sinh nhật. Admin tạo một khuyến mãi loại BIRTHDAY làm mẫu (mức giảm, đơn tối thiểu, giảm tối đa, hạng
 * áp dụng); {@link BirthdayVoucherScheduler} gọi {@link #issueDue} mỗi ngày và phát cho khách có tháng sinh là tháng
 * hiện tại một voucher riêng dùng từ ngày 1 đến hết tháng, online lẫn tại quầy. Mỗi khách nhận tối đa một voucher
 * mỗi năm (chỉ mục duy nhất theo {@code birthday_year}); khách thêm ngày sinh giữa tháng vẫn nhận ở lần chạy sau.
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.AuditInfrastructure(
        reason = "System-issued birthday vouchers; every issue is a customer_promotions row")
@Transactional(readOnly = true)
public class BirthdayVoucherService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NamedParameterJdbcTemplate jdbc;
    private final NotificationPublisher notifications;
    private final MailHelper mail;
    /** Kafka/SMTP chậm không được giữ luồng scheduler dùng chung với các job khác. */
    private final TaskExecutor delivery;

    public BirthdayVoucherService(NamedParameterJdbcTemplate jdbc, NotificationPublisher notifications, MailHelper mail,
            @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.mail = mail;
        this.delivery = delivery;
    }

    record Template(UUID id, String code, String name, String discountType, BigDecimal discountValue,
                    BigDecimal maxDiscount, BigDecimal minOrderValue) {
    }

    record Issued(UUID id, UUID customerId, String fullName, String email) {
    }

    /**
     * Phát voucher sinh nhật còn thiếu của tháng chứa {@code now} (giờ ứng dụng) theo mẫu BIRTHDAY đang chạy mới nhất.
     * Thông báo và email được gửi trên luồng nền sau khi commit.
     *
     * @return số voucher vừa phát
     */
    @Transactional
    public int issueDue(LocalDateTime now) {
        Template template = jdbc.query("""
                select id, code, name, discount_type, discount_value, max_discount, min_order_value from promotions
                where promotion_type = 'BIRTHDAY' and status = 'ACTIVE' and start_date <= :now and end_date > :now
                order by start_date desc, created_at desc limit 1""",
                new MapSqlParameterSource("now", DbTimes.toDb(now)), (rs, i) -> new Template(
                        rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                        rs.getString("discount_type"), rs.getBigDecimal("discount_value"),
                        rs.getBigDecimal("max_discount"), rs.getBigDecimal("min_order_value")))
                .stream().findFirst().orElse(null);
        if (template == null) return 0;

        YearMonth month = YearMonth.from(now);
        LocalDateTime validFrom = month.atDay(1).atStartOfDay();
        LocalDateTime expiresAt = month.plusMonths(1).atDay(1).atStartOfDay();
        List<Issued> issued = jdbc.query("""
                with inserted as (
                    insert into customer_promotions (customer_id, promotion_id, points_spent, redeemed_at, source,
                                                     valid_from, expires_at, birthday_year)
                    select c.id, :promotionId, 0, :now, 'BIRTHDAY', :validFrom, :expiresAt, :year
                    from customers c
                    where c.active and extract(month from c.date_of_birth) = :month
                      and not exists (select 1 from customer_promotions x where x.customer_id = c.id
                                      and x.source = 'BIRTHDAY' and x.birthday_year = :year)
                      and (not exists (select 1 from promotion_tiers pt where pt.promotion_id = :promotionId)
                           or exists (select 1 from promotion_tiers pt
                                      join customer_tier_assignments a on a.tier_id = pt.tier_id
                                      where pt.promotion_id = :promotionId and a.customer_id = c.id
                                        and a.expires_at is null))
                    on conflict (customer_id, birthday_year) where source = 'BIRTHDAY' do nothing
                    returning id, customer_id
                )
                select i.id, i.customer_id, c.full_name, c.email
                from inserted i join customers c on c.id = i.customer_id""",
                new MapSqlParameterSource("promotionId", template.id())
                        .addValue("now", DbTimes.toDb(now))
                        .addValue("validFrom", DbTimes.toDb(validFrom))
                        .addValue("expiresAt", DbTimes.toDb(expiresAt))
                        .addValue("year", month.getYear())
                        .addValue("month", month.getMonthValue()),
                (rs, i) -> new Issued(rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
                        rs.getString("full_name"), rs.getString("email")));
        if (issued.isEmpty()) return 0;
        LocalDateTime lastDay = expiresAt.minusDays(1);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                delivery.execute(() -> issued.forEach(voucher -> send(template, voucher, lastDay)));
            }
        });
        return issued.size();
    }

    private void send(Template template, Issued voucher, LocalDateTime lastDay) {
        String benefit = benefit(template);
        String message = "Lunaria tặng bạn voucher " + template.code() + " " + benefit
                + ", dùng được tới hết ngày " + lastDay.format(DATE) + " khi mua online hoặc tại cửa hàng.";
        try {
            notifications.publish(new NotificationRequest(List.of(RealtimeIdentity.customer(voucher.customerId())), null,
                    NotificationType.BIRTHDAY_VOUCHER, "Chúc mừng sinh nhật!", message,
                    "PROMOTION", template.id(), "/vouchers?tab=mine",
                    Map.of("voucherId", voucher.id().toString(), "promotionCode", template.code())));
        } catch (RuntimeException failure) {
            log.warn("Birthday voucher notification {} failed: {}", voucher.id(), failure.getMessage());
        }
        if (!StringUtils.hasText(voucher.email())) return;
        try {
            String name = StringUtils.hasText(voucher.fullName()) ? voucher.fullName().trim() : "bạn";
            mail.sendText(voucher.email(), "Chúc mừng sinh nhật " + name + " - quà từ Lunaria",
                    "Chào " + name + ",\n\nChúc bạn một tháng sinh nhật thật vui! " + message
                            + "\n\nVoucher đã có sẵn trong tài khoản của bạn, chỉ cần chọn khi thanh toán"
                            + " hoặc báo nhân viên tại quầy.\n\n— Lunaria Boutique");
        } catch (RuntimeException failure) {
            log.warn("Birthday voucher email {} failed: {}", voucher.id(), failure.getMessage());
        }
    }

    /** "giảm 15% (tối đa 100.000đ) cho đơn từ 300.000đ". */
    static String benefit(Template template) {
        StringBuilder text = new StringBuilder("giảm ");
        if ("PERCENT".equals(template.discountType())) {
            text.append(template.discountValue().stripTrailingZeros().toPlainString()).append('%');
            if (template.maxDiscount() != null) text.append(" (tối đa ").append(OrderCartPricer.vnd(template.maxDiscount())).append(')');
        } else {
            text.append(OrderCartPricer.vnd(template.discountValue()));
        }
        if (template.minOrderValue() != null && template.minOrderValue().signum() > 0)
            text.append(" cho đơn từ ").append(OrderCartPricer.vnd(template.minOrderValue()));
        return text.toString();
    }
}
