package com.fashionsystem.fashion_system.attendance;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lịch làm việc chung: ngày nghỉ hằng tuần (cấu hình) và ngày lễ. Ngày lễ rơi vào ngày làm việc là ngày nghỉ có lương,
 * không cần chấm công. Dùng cho số ngày nghỉ phép, loại tăng ca và ngày công chuẩn của bảng lương.
 */
@Component
public class WorkCalendar {
    private final NamedParameterJdbcTemplate jdbc;

    public WorkCalendar(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ảnh chụp lịch cho một khoảng ngày (đọc DB một lần). */
    public record Snapshot(Set<DayOfWeek> weeklyOff, Map<LocalDate, String> holidays) {
        public boolean weeklyOff(LocalDate date) {
            return weeklyOff.contains(date.getDayOfWeek());
        }

        public boolean holiday(LocalDate date) {
            return holidays.containsKey(date);
        }

        /** Ngày làm việc theo lịch (gồm cả ngày lễ rơi vào ngày làm việc). */
        public boolean workingDay(LocalDate date) {
            return !weeklyOff(date);
        }

        public String overtimeType(LocalDate date) {
            if (holiday(date)) return "HOLIDAY";
            return weeklyOff(date) ? "WEEKEND" : "WEEKDAY";
        }

        public int workingDays(LocalDate from, LocalDate to) {
            int count = 0;
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) if (workingDay(d)) count++;
            return count;
        }

        /**
         * Số ngày nghỉ phép thực tế: chỉ tính ngày làm việc không phải lễ; nửa ngày đầu / cuối trừ 0.5 nếu ngày đó
         * được tính.
         */
        public BigDecimal leaveDays(LocalDate from, LocalDate to, boolean halfStart, boolean halfEnd) {
            BigDecimal total = BigDecimal.ZERO;
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                if (!workingDay(d) || holiday(d)) continue;
                boolean half = (halfStart && d.equals(from)) || (halfEnd && d.equals(to));
                total = total.add(half ? new BigDecimal("0.5") : BigDecimal.ONE);
            }
            return total;
        }
    }

    public Snapshot snapshot(LocalDate from, LocalDate to) {
        List<Integer> off = jdbc.queryForList("select unnest(weekly_off_days)::int from attendance_settings where id = 1",
                new MapSqlParameterSource(), Integer.class);
        Map<LocalDate, String> holidays = new HashMap<>();
        jdbc.query("select holiday_date, name from public_holidays where holiday_date between :from and :to",
                new MapSqlParameterSource("from", from).addValue("to", to),
                rs -> {
                    holidays.put(rs.getObject("holiday_date", LocalDate.class), rs.getString("name"));
                });
        Set<DayOfWeek> weeklyOff = off.stream().map(DayOfWeek::of).collect(Collectors.toCollection(TreeSet::new));
        return new Snapshot(weeklyOff, holidays);
    }

    public Snapshot snapshot(LocalDate date) {
        return snapshot(date, date);
    }
}
