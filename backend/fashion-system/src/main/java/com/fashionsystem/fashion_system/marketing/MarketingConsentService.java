package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tùy chọn nhận tin marketing của khách.
 * <ul>
 *   <li>Email khuyến mãi là opt-in: mặc định KHÔNG gửi; khách tự bật trong hồ sơ (cần có email). Hủy đăng ký bằng link
 *       trong mọi email, không cần đăng nhập.</li>
 *   <li>Thông báo marketing trong app mặc định bật; khách tự tắt được. Không ảnh hưởng thông báo về đơn hàng.</li>
 *   <li>Mọi lần đồng ý / rút lại được ghi {@code marketing_consent_events} để chứng minh sự đồng ý.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketingConsentService {
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * @param email     email hiện tại của khách đã che bớt; null khi khách chưa có email (không bật được email khuyến mãi)
     * @param inAppEnabled nhận thông báo marketing trong app
     */
    public record Preferences(String email, boolean emailOptIn, LocalDateTime emailConsentedAt, boolean inAppEnabled) {}

    public record PreferencesRequest(boolean emailOptIn, boolean inAppEnabled) {}

    /** Thông tin hiển thị trên trang hủy đăng ký (không lộ email đầy đủ). */
    public record UnsubscribeInfo(String email, boolean subscribed) {}

    public Preferences preferences(UUID customerId) {
        Map<String, Object> row = row(customerId);
        return new Preferences(maskEmail((String) row.get("email")), Boolean.TRUE.equals(row.get("email_opt_in")),
                DbTimes.fromDb(row.get("email_consented_at")), !Boolean.TRUE.equals(row.get("in_app_opt_out")));
    }

    @Transactional
    @BusinessAudit("MARKETING_CONSENT")
    public Preferences update(UUID customerId, PreferencesRequest request) {
        Map<String, Object> current = row(customerId);
        String email = (String) current.get("email");
        if (request.emailOptIn() && (email == null || email.isBlank()))
            throw BusinessException.conflict("Tài khoản chưa có email; hãy thêm email trong hồ sơ trước khi đăng ký nhận tin");
        boolean wasEmail = Boolean.TRUE.equals(current.get("email_opt_in"));
        boolean wasInApp = !Boolean.TRUE.equals(current.get("in_app_opt_out"));
        ensureRow(customerId);
        jdbc.update("""
                update marketing_customer_preferences set email_opt_in = :email, in_app_opt_out = :inAppOff,
                       email_consented_at = case when :email and not email_opt_in then (now() at time zone 'UTC') else email_consented_at end,
                       email_unsubscribed_at = case when not :email and email_opt_in then (now() at time zone 'UTC') else email_unsubscribed_at end,
                       updated_at = (now() at time zone 'UTC')
                where customer_id = :customer""",
                new MapSqlParameterSource("customer", customerId).addValue("email", request.emailOptIn())
                        .addValue("inAppOff", !request.inAppEnabled()));
        if (wasEmail != request.emailOptIn()) event(customerId, "EMAIL", request.emailOptIn(), "CUSTOMER_PROFILE");
        if (wasInApp != request.inAppEnabled()) event(customerId, "IN_APP", request.inAppEnabled(), "CUSTOMER_PROFILE");
        return preferences(customerId);
    }

    public UnsubscribeInfo unsubscribeInfo(UUID token) {
        Map<String, Object> row = byToken(token);
        return new UnsubscribeInfo(maskEmail((String) row.get("email")), Boolean.TRUE.equals(row.get("email_opt_in")));
    }

    /** Hủy nhận email khuyến mãi bằng token trong email; gọi lại nhiều lần vẫn an toàn. */
    @Transactional
    @BusinessAudit("MARKETING_CONSENT")
    public UnsubscribeInfo unsubscribe(UUID token) {
        Map<String, Object> row = byToken(token);
        if (Boolean.TRUE.equals(row.get("email_opt_in"))) {
            UUID customer = (UUID) row.get("customer_id");
            jdbc.update("""
                    update marketing_customer_preferences set email_opt_in = false, email_unsubscribed_at = (now() at time zone 'UTC'),
                           updated_at = (now() at time zone 'UTC') where customer_id = :customer""",
                    new MapSqlParameterSource("customer", customer));
            event(customer, "EMAIL", false, "UNSUBSCRIBE_LINK");
        }
        return new UnsubscribeInfo(maskEmail((String) row.get("email")), false);
    }

    private Map<String, Object> row(UUID customerId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select c.email, mp.email_opt_in, mp.email_consented_at, mp.in_app_opt_out
                from customers c left join marketing_customer_preferences mp on mp.customer_id = c.id where c.id = :id""",
                new MapSqlParameterSource("id", customerId));
        if (rows.isEmpty()) throw BusinessException.notFound("Khách hàng không tồn tại");
        return rows.getFirst();
    }

    private Map<String, Object> byToken(UUID token) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select mp.customer_id, mp.email_opt_in, c.email from marketing_customer_preferences mp
                join customers c on c.id = mp.customer_id where mp.unsubscribe_token = :token""",
                new MapSqlParameterSource("token", token));
        if (rows.isEmpty()) throw BusinessException.notFound("Link hủy đăng ký không hợp lệ hoặc đã hết hiệu lực");
        return rows.getFirst();
    }

    private void ensureRow(UUID customerId) {
        jdbc.update("insert into marketing_customer_preferences(customer_id) values (:customer) on conflict (customer_id) do nothing",
                new MapSqlParameterSource("customer", customerId));
    }

    private void event(UUID customerId, String channel, boolean granted, String source) {
        jdbc.update("""
                insert into marketing_consent_events(customer_id, channel, action, source, created_at)
                values (:customer, :channel, :action, :source, (clock_timestamp() at time zone 'UTC'))""",
                new MapSqlParameterSource("customer", customerId).addValue("channel", channel)
                        .addValue("action", granted ? "GRANTED" : "WITHDRAWN").addValue("source", source));
    }

    static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
        return email.charAt(0) + "***" + email.substring(at);
    }
}
