package com.fashionsystem.fashion_system.attendance;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fashionsystem.fashion_system.util.DbTimes;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tổng hợp dữ liệu công đã xử lý của một nhân viên trong một khoảng ngày — đầu vào cho bảng lương. Chỉ dùng dữ liệu
 * hợp lệ: bản ghi đủ giờ vào / ra, đơn nghỉ và tăng ca ĐÃ DUYỆT; đơn đang chờ chỉ được đếm để HR xem xét, không tính
 * vào công. Ngày làm việc không có dữ liệu (hoặc thiếu giờ vào / ra) là "chưa xác minh", không tự động bị trừ lương.
 */
@Service
@Transactional(readOnly = true)
public class AttendanceSummaryService {
    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final NamedParameterJdbcTemplate jdbc;
    private final WorkCalendar calendar;

    public AttendanceSummaryService(NamedParameterJdbcTemplate jdbc, WorkCalendar calendar) {
        this.jdbc = jdbc;
        this.calendar = calendar;
    }

    /**
     * Phân loại một ngày. {@code payable} = phần ngày được trả lương theo lịch (1 − nghỉ không lương), chỉ với ngày
     * làm việc; ngày chưa xác minh vẫn được trả cho tới khi HR xác nhận vắng.
     */
    public record Day(LocalDate date, boolean workingDay, String holiday, BigDecimal worked, BigDecimal paidLeave,
                      BigDecimal unpaidLeave, BigDecimal unverified, String leaveType, boolean offDayWorked, String recordStatus,
                      BigDecimal payable) {
    }

    public record Summary(LocalDate from, LocalDate to, int workdays, BigDecimal workedDays, BigDecimal offDayWorkedDays,
                          int workedMinutes, BigDecimal paidLeaveDays, BigDecimal unpaidLeaveDays, BigDecimal holidayDays,
                          BigDecimal unverifiedDays, int lateCount, int earlyCount, int lateMinutes, int earlyMinutes,
                          int missingCheckOut, int missingCheckIn, int approvedExcuses, int pendingRequests,
                          BigDecimal otWeekdayHours, BigDecimal otWeekendHours, BigDecimal otHolidayHours, List<Day> days) {
    }

    private record Rec(String status, LocalDateTime in, LocalDateTime out, int late, int early) {
    }

    private record LeaveRow(LocalDate start, LocalDate end, boolean halfStart, boolean halfEnd, boolean paid, String type) {
    }

    public Summary summarize(UUID userId, LocalDate from, LocalDate to) {
        MapSqlParameterSource p = new MapSqlParameterSource("u", userId).addValue("from", from).addValue("to", to);
        WorkCalendar.Snapshot cal = calendar.snapshot(from, to);
        Map<LocalDate, Rec> records = new HashMap<>();
        jdbc.query("""
                select attendance_date, status, check_in_time, check_out_time, late_minutes, early_minutes
                from attendance_records where user_id = :u and attendance_date between :from and :to
                """, p, rs -> {
            records.put(rs.getObject("attendance_date", LocalDate.class), new Rec(rs.getString("status"),
                    DbTimes.fromDb(rs.getTimestamp("check_in_time")), DbTimes.fromDb(rs.getTimestamp("check_out_time")),
                    rs.getInt("late_minutes"), rs.getInt("early_minutes")));
        });
        List<LeaveRow> leaves = jdbc.query("""
                select l.start_date, l.end_date, l.is_half_day_start, l.is_half_day_end, t.paid, t.name
                from leave_requests l join leave_types t on t.id = l.leave_type_id
                where l.user_id = :u and l.status = 'APPROVED' and l.start_date <= :to and l.end_date >= :from
                """, p, (rs, i) -> new LeaveRow(rs.getObject(1, LocalDate.class), rs.getObject(2, LocalDate.class),
                rs.getBoolean(3), rs.getBoolean(4), rs.getBoolean(5), rs.getString(6)));

        List<Day> days = new ArrayList<>();
        BigDecimal worked = BigDecimal.ZERO, offWorked = BigDecimal.ZERO, paidLeave = BigDecimal.ZERO, unpaidLeave = BigDecimal.ZERO,
                holidays = BigDecimal.ZERO, unverified = BigDecimal.ZERO;
        int workdays = 0, minutes = 0, lateCount = 0, earlyCount = 0, lateMin = 0, earlyMin = 0, missOut = 0, missIn = 0;
        LocalDate today = LocalDate.now();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Rec r = records.get(d);
            boolean complete = r != null && "COMPLETED".equals(r.status());
            if (r != null) {
                if (complete) minutes += (int) Duration.between(r.in(), r.out()).toMinutes();
                if (r.late() > 0) { lateCount++; lateMin += r.late(); }
                if (r.early() > 0) { earlyCount++; earlyMin += r.early(); }
                if ("MISSING_CHECK_OUT".equals(r.status()) && d.isBefore(today)) missOut++;
                if ("MISSING_CHECK_IN".equals(r.status())) missIn++;
            }
            String holiday = cal.holidays().get(d);
            if (!cal.workingDay(d)) {
                if (complete) offWorked = offWorked.add(BigDecimal.ONE);
                days.add(new Day(d, false, holiday, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
                        complete, r == null ? null : r.status(), BigDecimal.ZERO));
                continue;
            }
            workdays++;
            if (holiday != null) {
                holidays = holidays.add(BigDecimal.ONE);
                if (complete) worked = worked.add(BigDecimal.ONE);
                days.add(new Day(d, true, holiday, complete ? BigDecimal.ONE : BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, null, false, r == null ? null : r.status(), BigDecimal.ONE));
                continue;
            }
            BigDecimal leaveFraction = BigDecimal.ZERO;
            LeaveRow leave = null;
            for (LeaveRow l : leaves) {
                if (d.isBefore(l.start()) || d.isAfter(l.end())) continue;
                boolean half = (l.halfStart() && d.equals(l.start())) || (l.halfEnd() && d.equals(l.end()));
                leaveFraction = half ? HALF : BigDecimal.ONE;
                leave = l;
                break;
            }
            BigDecimal dayPaidLeave = leave != null && leave.paid() ? leaveFraction : BigDecimal.ZERO;
            BigDecimal dayUnpaid = leave != null && !leave.paid() ? leaveFraction : BigDecimal.ZERO;
            BigDecimal rest = BigDecimal.ONE.subtract(leaveFraction);
            BigDecimal dayWorked = complete ? rest : BigDecimal.ZERO;
            BigDecimal dayUnverified = complete ? BigDecimal.ZERO : rest;
            paidLeave = paidLeave.add(dayPaidLeave);
            unpaidLeave = unpaidLeave.add(dayUnpaid);
            worked = worked.add(dayWorked);
            unverified = unverified.add(dayUnverified);
            days.add(new Day(d, true, null, dayWorked, dayPaidLeave, dayUnpaid, dayUnverified, leave == null ? null : leave.type(),
                    false, r == null ? null : r.status(), BigDecimal.ONE.subtract(dayUnpaid)));
        }

        Map<String, Object> counts = jdbc.queryForMap("""
                select
                  (select count(*) from excuse_requests where user_id = :u and status = 'APPROVED' and attendance_date between :from and :to) as excuses,
                  (select count(*) from excuse_requests where user_id = :u and status = 'PENDING' and attendance_date between :from and :to)
                + (select count(*) from leave_requests where user_id = :u and status = 'PENDING' and start_date <= :to and end_date >= :from)
                + (select count(*) from overtime_requests where user_id = :u and status = 'PENDING' and work_date between :from and :to) as pending,
                  coalesce((select sum(approved_hours) from overtime_requests where user_id = :u and status = 'APPROVED'
                            and overtime_type = 'WEEKDAY' and work_date between :from and :to), 0) as ot_weekday,
                  coalesce((select sum(approved_hours) from overtime_requests where user_id = :u and status = 'APPROVED'
                            and overtime_type = 'WEEKEND' and work_date between :from and :to), 0) as ot_weekend,
                  coalesce((select sum(approved_hours) from overtime_requests where user_id = :u and status = 'APPROVED'
                            and overtime_type = 'HOLIDAY' and work_date between :from and :to), 0) as ot_holiday
                """, p);
        return new Summary(from, to, workdays, worked, offWorked, minutes, paidLeave, unpaidLeave, holidays, unverified, lateCount,
                earlyCount, lateMin, earlyMin, missOut, missIn, ((Number) counts.get("excuses")).intValue(),
                ((Number) counts.get("pending")).intValue(), (BigDecimal) counts.get("ot_weekday"), (BigDecimal) counts.get("ot_weekend"),
                (BigDecimal) counts.get("ot_holiday"), days);
    }

    /** Số ngày làm việc theo lịch trong một khoảng (ngày công chuẩn chế độ CALENDAR). */
    public int workingDays(LocalDate from, LocalDate to) {
        return calendar.snapshot(from, to).workingDays(from, to);
    }
}
