package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.exception.BusinessException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Kỳ lương đã gửi duyệt khóa dữ liệu công của nhân viên trong tháng đó: không chấm công bổ sung, điều chỉnh, duyệt
 * giải trình / nghỉ phép / tăng ca cho những ngày thuộc tháng bị khóa.
 */
@Component
public class AttendanceLockService {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private final NamedParameterJdbcTemplate jdbc;

    public AttendanceLockService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean locked(UUID userId, LocalDate date) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from attendance_locks where user_id = :u and period_month = :m)",
                new MapSqlParameterSource("u", userId).addValue("m", date.withDayOfMonth(1)), Boolean.class));
    }

    public void requireUnlocked(UUID userId, LocalDate date) {
        if (locked(userId, date))
            throw BusinessException.conflict("Dữ liệu công tháng " + date.format(MONTH)
                    + " đã chốt để tính lương, không thể thay đổi. Liên hệ HR để tạo điều chỉnh lương.");
    }

    public void requireUnlocked(UUID userId, LocalDate from, LocalDate to) {
        for (LocalDate m = from.withDayOfMonth(1); !m.isAfter(to); m = m.plusMonths(1)) requireUnlocked(userId, m);
    }
}
