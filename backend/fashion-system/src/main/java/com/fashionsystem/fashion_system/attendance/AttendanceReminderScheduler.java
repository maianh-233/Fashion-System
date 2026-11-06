package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nhắc chấm công: nhắc check-in quanh giờ vào ca, nhắc check-out trước giờ tan ca, nhắc khi đã quá giờ tan ca mà vẫn
 * chưa check-out. Nhân viên làm theo lịch ca chỉ được nhắc ngày có ca; người khác bỏ qua ngày nghỉ tuần / lễ.
 * Bỏ qua nhân viên đang nghỉ phép. Mỗi loại nhắc tối đa một lần / ngày
 * (lưu trong attendance_reminders); thông báo gửi trên luồng nền để không chặn khi Kafka chậm.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "attendance.reminder.enabled", havingValue = "true", matchIfMissing = true)
public class AttendanceReminderScheduler {
    static final String TYPE = "ATTENDANCE_REMINDER";

    private final NamedParameterJdbcTemplate jdbc;
    private final AttendancePolicy policy;
    private final WorkCalendar calendar;
    private final NotificationPublisher publisher;
    private final TaskExecutor delivery;

    public AttendanceReminderScheduler(NamedParameterJdbcTemplate jdbc, AttendancePolicy policy, WorkCalendar calendar,
                                       NotificationPublisher publisher,
                                       @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor delivery) {
        this.jdbc = jdbc;
        this.policy = policy;
        this.calendar = calendar;
        this.publisher = publisher;
        this.delivery = delivery;
    }

    /**
     * @param scheduledToday có ca đã công bố hôm nay
     * @param scheduledThisWeek có lịch xếp ca đã công bố trong tuần (nhân viên làm theo lịch ca)
     */
    private record Candidate(UUID userId, UUID storeId, boolean checkedIn, boolean checkedOut, boolean scheduledToday,
                             boolean scheduledThisWeek) {
        /** Người làm theo lịch ca: chỉ nhắc ngày có ca (kể cả ngày nghỉ tuần / lễ); còn lại theo lịch làm việc chung. */
        boolean worksToday(boolean calendarOff) {
            if (scheduledToday) return true;
            return !scheduledThisWeek && !calendarOff;
        }
    }

    @Scheduled(cron = "${attendance.reminder.cron:0 */5 * * * *}")
    public void remind() {
        LocalDate today = LocalDate.now();
        WorkCalendar.Snapshot cal = calendar.snapshot(today);
        boolean calendarOff = cal.weeklyOff(today) || cal.holiday(today);
        LocalTime now = LocalTime.now();
        List<Candidate> candidates = jdbc.query("""
                select u.id, (select ss.store_id from store_staffs ss where ss.user_id = u.id and ss.active limit 1) as store_id,
                       r.check_in_time, r.check_out_time,
                       exists (select 1 from shift_schedules s where s.user_id = u.id and s.status = 'PUBLISHED'
                               and s.work_date = :d) as scheduled_today,
                       exists (select 1 from shift_schedules s where s.user_id = u.id and s.status = 'PUBLISHED'
                               and s.work_date between :weekStart and :weekEnd) as scheduled_week
                from users u
                left join attendance_records r on r.user_id = u.id and r.attendance_date = :d
                where %s and u.employee_code is not null
                  and not exists (select 1 from leave_requests l where l.user_id = u.id and l.status = 'APPROVED'
                                  and :d between l.start_date and l.end_date)
                """.formatted(AttendanceAdminService.ACTIVE_EMPLOYEE), new MapSqlParameterSource("d", today)
                        .addValue("weekStart", today.with(java.time.DayOfWeek.MONDAY))
                        .addValue("weekEnd", today.with(java.time.DayOfWeek.SUNDAY)),
                (rs, i) -> new Candidate(rs.getObject("id", UUID.class), rs.getObject("store_id", UUID.class),
                        rs.getTimestamp("check_in_time") != null, rs.getTimestamp("check_out_time") != null,
                        rs.getBoolean("scheduled_today"), rs.getBoolean("scheduled_week")));
        for (Candidate c : candidates) {
            if (!c.worksToday(calendarOff)) continue;
            try {
                AttendancePolicy.Rule rule = policy.ruleFor(c.userId(), c.storeId(), today);
                if (rule.end().isBefore(rule.start())) continue;
                if (!c.checkedIn() && !c.checkedOut() && !now.isBefore(rule.start().minusMinutes(10))
                        && now.isBefore(rule.start().plusMinutes(60)))
                    send(c.userId(), today, "CHECK_IN", "Nhắc check-in", "Ca làm bắt đầu lúc " + rule.start() + ". Đừng quên check-in.");
                else if (c.checkedIn() && !c.checkedOut() && !now.isBefore(rule.end().minusMinutes(15))
                        && now.isBefore(rule.end()))
                    send(c.userId(), today, "CHECK_OUT", "Nhắc check-out", "Ca làm kết thúc lúc " + rule.end() + ". Nhớ check-out khi về.");
                else if (c.checkedIn() && !c.checkedOut() && !now.isBefore(rule.end().plusMinutes(60)))
                    send(c.userId(), today, "STILL_CHECKED_IN", "Bạn chưa check-out",
                            "Đã quá giờ tan ca " + rule.end() + " nhưng bạn chưa check-out. Nếu đã về, hãy gửi đơn giải trình.");
            } catch (RuntimeException e) {
                log.warn("Attendance reminder failed for {}", c.userId(), e);
            }
        }
    }

    private void send(UUID userId, LocalDate day, String kind, String title, String message) {
        int inserted = jdbc.update("""
                insert into attendance_reminders(user_id, reminder_type, reminder_date, sent_at)
                select :u, :k, :d, now() at time zone 'UTC'
                where not exists (select 1 from attendance_reminders where user_id = :u and reminder_type = :k and reminder_date = :d)
                """, new MapSqlParameterSource("u", userId).addValue("k", kind).addValue("d", day));
        if (inserted == 0) return;
        NotificationRequest request = new NotificationRequest(List.of(RealtimeIdentity.user(userId)), null, TYPE, title, message,
                "ATTENDANCE", null, "/admin/me/attendance", Map.of());
        delivery.execute(() -> {
            try {
                publisher.publish(request);
            } catch (RuntimeException e) {
                log.warn("Attendance reminder notification failed: {}", title, e);
            }
        });
    }
}
