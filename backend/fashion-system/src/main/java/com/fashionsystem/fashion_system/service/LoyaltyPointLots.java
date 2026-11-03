package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.util.DbTimes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Các đợt điểm của khách (bảng {@code loyalty_point_lots}). Mỗi lần tích là một đợt hết hạn sau
 * {@code loyalty.points-valid-months} tháng; mọi khoản trừ lấy từ đợt sắp hết hạn trước. Gọi trong giao dịch đã khóa
 * ví điểm của khách để tổng điểm còn lại của các đợt luôn khớp {@code loyalty_accounts.points_balance}.
 */
@Component
public class LoyaltyPointLots {
    private final NamedParameterJdbcTemplate jdbc;

    @Value("${loyalty.points-valid-months:12}")
    private int validMonths = 12;

    public LoyaltyPointLots(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record Lot(UUID id, int remaining) {
    }

    /** Đợt điểm mới; {@code orderId} là đơn được tích (null với điểm không từ đơn). */
    public void add(UUID customerId, UUID orderId, int points, LocalDateTime earnedAt, String note) {
        if (points <= 0) return;
        jdbc.update("""
                insert into loyalty_point_lots (customer_id, order_id, points_earned, points_remaining, earned_at,
                                                expires_at, note)
                values (:customerId, :orderId, :points, :points, :earnedAt, :expiresAt, :note)""",
                new MapSqlParameterSource("customerId", customerId).addValue("orderId", orderId).addValue("points", points)
                        .addValue("earnedAt", DbTimes.toDb(earnedAt))
                        .addValue("expiresAt", DbTimes.toDb(expiryFor(earnedAt, validMonths))).addValue("note", note));
    }

    /** Dùng được tới hết ngày tương ứng sau {@code months} tháng (hết hạn lúc nửa đêm, giờ ứng dụng). */
    static LocalDateTime expiryFor(LocalDateTime earnedAt, int months) {
        return earnedAt.toLocalDate().plusMonths(months).plusDays(1).atStartOfDay();
    }

    /**
     * Trừ {@code points} điểm: từ đợt của {@code preferredOrderId} trước (nếu có), sau đó các đợt sắp hết hạn trước.
     *
     * @return số điểm thực sự trừ được (nhỏ hơn khi các đợt không đủ)
     */
    public int consume(UUID customerId, int points, UUID preferredOrderId) {
        if (points <= 0) return 0;
        List<Lot> lots = jdbc.query("""
                select id, points_remaining from loyalty_point_lots
                where customer_id = :customerId and points_remaining > 0
                order by coalesce(order_id = cast(:preferred as uuid), false) desc, expires_at, earned_at, id
                for update""",
                new MapSqlParameterSource("customerId", customerId).addValue("preferred", preferredOrderId),
                (rs, i) -> new Lot(rs.getObject("id", UUID.class), rs.getInt("points_remaining")));
        int left = points;
        for (Lot lot : lots) {
            if (left <= 0) break;
            int take = Math.min(left, lot.remaining());
            jdbc.update("update loyalty_point_lots set points_remaining = points_remaining - :take where id = :id",
                    new MapSqlParameterSource("take", take).addValue("id", lot.id()));
            left -= take;
        }
        return points - left;
    }

    /** Số điểm của đơn đã hết hạn (không được trừ lại khi hoàn tiền đơn đó). */
    public int expiredOfOrder(UUID orderId) {
        return jdbc.queryForList("select points_expired from loyalty_point_lots where order_id = :orderId",
                new MapSqlParameterSource("orderId", orderId), Integer.class).stream().findFirst().orElse(0);
    }
}
