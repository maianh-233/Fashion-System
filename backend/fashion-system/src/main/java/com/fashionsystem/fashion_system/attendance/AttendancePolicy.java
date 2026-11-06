package com.fashionsystem.fashion_system.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Giờ làm dự kiến của một nhân viên trong một ngày: lịch xếp ca đã công bố → ca được phân công cố định → cấu hình
 * cửa hàng → mặc định toàn chuỗi
 * (08:00–18:00 nếu chưa cấu hình). Ngưỡng trễ / về sớm lấy theo cùng thứ tự.
 */
@Component
public class AttendancePolicy {
    private static final Rule FALLBACK = new Rule(null, LocalTime.of(8, 0), LocalTime.of(18, 0), 5, 5);

    private final NamedParameterJdbcTemplate jdbc;

    public AttendancePolicy(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Rule(UUID shiftId, LocalTime start, LocalTime end, int lateThreshold, int earlyThreshold) {
        /** Số phút đi trễ (0 nếu trong ngưỡng cho phép). */
        public int lateMinutes(LocalTime checkIn) {
            long minutes = java.time.Duration.between(start, checkIn).toMinutes();
            return minutes > lateThreshold ? (int) minutes : 0;
        }

        /** Số phút về sớm (0 nếu trong ngưỡng); ca qua đêm không tính về sớm. */
        public int earlyMinutes(LocalTime checkOut) {
            if (end.isBefore(start)) return 0;
            long minutes = java.time.Duration.between(checkOut, end).toMinutes();
            return minutes > earlyThreshold ? (int) minutes : 0;
        }
    }

    public record Settings(int storeRadiusMeters, boolean requireStoreLocation, BigDecimal standardHoursPerDay) {
    }

    public Settings settings() {
        return jdbc.queryForObject("select store_radius_meters, require_store_location, standard_hours_per_day from attendance_settings where id = 1",
                new MapSqlParameterSource(), (rs, i) -> new Settings(rs.getInt(1), rs.getBoolean(2), rs.getBigDecimal(3)));
    }

    public Rule ruleFor(UUID userId, UUID storeId, LocalDate date) {
        // Lịch xếp ca đã công bố cho ngày này được ưu tiên hơn mọi cấu hình.
        List<Rule> scheduled = jdbc.query("""
                select ss.shift_id, ss.start_time, ss.end_time,
                       coalesce(sc.late_threshold_minutes, st.late_threshold_minutes, d.late_threshold_minutes, 5) as late_t,
                       coalesce(sc.early_departure_threshold_minutes, st.early_departure_threshold_minutes,
                                d.early_departure_threshold_minutes, 5) as early_t
                from shift_schedules ss
                left join attendance_configs sc on sc.shift_id = ss.shift_id
                left join attendance_configs st on st.store_id = ss.store_id and st.shift_id is null
                left join attendance_configs d on d.store_id is null and d.shift_id is null
                where ss.user_id = :user and ss.work_date = :date and ss.status = 'PUBLISHED'
                limit 1
                """, new MapSqlParameterSource("user", userId).addValue("date", date),
                (rs, i) -> new Rule(rs.getObject("shift_id", UUID.class), rs.getObject("start_time", LocalTime.class),
                        rs.getObject("end_time", LocalTime.class), rs.getInt("late_t"), rs.getInt("early_t")));
        if (!scheduled.isEmpty()) return scheduled.getFirst();
        List<Rule> shift = jdbc.query("""
                select s.id, s.start_time, s.end_time,
                       coalesce(sc.late_threshold_minutes, st.late_threshold_minutes, d.late_threshold_minutes, 5) as late_t,
                       coalesce(sc.early_departure_threshold_minutes, st.early_departure_threshold_minutes,
                                d.early_departure_threshold_minutes, 5) as early_t
                from employee_shifts es
                join shifts s on s.id = es.shift_id and s.active
                left join attendance_configs sc on sc.shift_id = s.id
                left join attendance_configs st on st.store_id = cast(:store as uuid) and st.shift_id is null
                left join attendance_configs d on d.store_id is null and d.shift_id is null
                where es.user_id = :user and es.start_date <= :date and (es.end_date is null or es.end_date >= :date)
                order by es.start_date desc limit 1
                """, new MapSqlParameterSource("user", userId).addValue("date", date).addValue("store", storeId, java.sql.Types.OTHER),
                (rs, i) -> new Rule(rs.getObject("id", UUID.class), rs.getObject("start_time", LocalTime.class),
                        rs.getObject("end_time", LocalTime.class), rs.getInt("late_t"), rs.getInt("early_t")));
        if (!shift.isEmpty()) return shift.getFirst();
        List<Rule> config = jdbc.query("""
                select check_in_time, check_out_time, late_threshold_minutes, early_departure_threshold_minutes
                from attendance_configs where shift_id is null and (store_id = cast(:store as uuid) or store_id is null)
                order by store_id nulls last limit 1
                """, new MapSqlParameterSource().addValue("store", storeId, java.sql.Types.OTHER),
                (rs, i) -> new Rule(null, rs.getObject(1, LocalTime.class), rs.getObject(2, LocalTime.class), rs.getInt(3), rs.getInt(4)));
        return config.isEmpty() ? FALLBACK : config.getFirst();
    }

    public record StoreLocation(BigDecimal latitude, BigDecimal longitude, String name) {
    }

    public StoreLocation storeLocation(UUID storeId) {
        List<StoreLocation> rows = jdbc.query("select latitude, longitude, name from stores where id = :id",
                new MapSqlParameterSource("id", storeId),
                (rs, i) -> new StoreLocation(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getString(3)));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** Khoảng cách (mét) giữa hai tọa độ, công thức haversine. */
    static int distanceMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double r = 6_371_000;
        double p1 = Math.toRadians(lat1.doubleValue());
        double p2 = Math.toRadians(lat2.doubleValue());
        double dp = p2 - p1;
        double dl = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return (int) Math.round(2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
