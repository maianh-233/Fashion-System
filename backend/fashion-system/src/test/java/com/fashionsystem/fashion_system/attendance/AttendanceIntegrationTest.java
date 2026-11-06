package com.fashionsystem.fashion_system.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Chấm công trên cơ sở dữ liệu thật; dữ liệu rollback sau mỗi test. */
@SpringBootTest
@Transactional
class AttendanceIntegrationTest {
    @Autowired AttendanceRecordService records;
    @Autowired ExcuseRequestService excuses;
    @Autowired LeaveRequestService leaves;
    @Autowired OvertimeRequestService overtimes;
    @Autowired AttendanceSummaryService summaries;
    @Autowired JdbcTemplate jdbc;

    final String tag = UUID.randomUUID().toString().substring(0, 6);
    final UUID storeA = UUID.randomUUID();
    final UUID storeB = UUID.randomUUID();
    final UUID manager = UUID.randomUUID();
    final UUID staffA = UUID.randomUUID();
    final UUID staffB = UUID.randomUUID();
    final UUID globalEmp = UUID.randomUUID();
    /** Tọa độ riêng cho cửa hàng thử (bảng stores không cho trùng tọa độ). */
    final BigDecimal latA = new BigDecimal("11.").add(BigDecimal.valueOf(Math.abs(UUID.randomUUID().getLeastSignificantBits() % 400000), 6));
    AttendanceAccess.Actor storeManager;
    AttendanceAccess.Actor centralHr;

    @BeforeEach
    void setUp() {
        jdbc.update("update attendance_settings set weekly_off_days = '{7}', store_radius_meters = 300, require_store_location = true where id = 1");
        jdbc.update("update attendance_configs set check_in_time = '08:00', check_out_time = '18:00', late_threshold_minutes = 5, "
                + "early_departure_threshold_minutes = 5 where store_id is null and shift_id is null");
        store(storeA, "A", latA);
        store(storeB, "B", latA.add(new BigDecimal("0.5")));
        user(manager, "mgr");
        user(staffA, "sa");
        user(staffB, "sb");
        user(globalEmp, "gl");
        staff(manager, storeA);
        staff(staffA, storeA);
        staff(staffB, storeB);
        storeManager = new AttendanceAccess.Actor(manager, storeA, "A", true, true, true, true, false);
        centralHr = new AttendanceAccess.Actor(UUID.randomUUID(), null, null, true, true, true, true, true);
    }

    @Test
    void storeEmployeeMustCheckInAtAssignedStoreAndOnlyOncePerDay() {
        assertThatThrownBy(() -> job(() -> records.checkIn(staffA, new AttendanceDtos.CheckRequest(null, null, null))))
                .hasMessageContaining("bật định vị");
        assertThatThrownBy(() -> job(() -> records.checkIn(staffA,
                new AttendanceDtos.CheckRequest(latA.add(new BigDecimal("0.03")), new BigDecimal("106.700000"), null))))
                .hasMessageContaining("cách cửa hàng");
        AttendanceDtos.Record record = job(() -> records.checkIn(staffA,
                new AttendanceDtos.CheckRequest(latA.add(new BigDecimal("0.0005")), new BigDecimal("106.700000"), storeB)));
        assertThat(record.storeId()).isEqualTo(storeA);                  // nhân viên cửa hàng không chọn cửa hàng khác
        assertThat(record.checkInDistanceM()).isLessThan(300);
        assertThatThrownBy(() -> job(() -> records.checkIn(staffA,
                new AttendanceDtos.CheckRequest(latA, new BigDecimal("106.700000"), null))))
                .hasMessageContaining("đã check-in");
        // Nhân viên Global: không cần định vị, không gắn cửa hàng.
        AttendanceDtos.Record global = job(() -> records.checkIn(globalEmp, null));
        assertThat(global.employeeType()).isEqualTo("GLOBAL");
        assertThat(global.storeId()).isNull();
    }

    @Test
    void approvedExcuseUpdatesAttendanceWithAuditTrail() {
        LocalDate day = LocalDate.now().minusDays(2);
        jdbc.update("""
                insert into attendance_records(user_id, attendance_date, store_id, check_in_time, status, expected_start, expected_end, created_at)
                values (?, ?, ?, ?, 'MISSING_CHECK_OUT', '08:00', '18:00', now())
                """, staffA, day, storeA, com.fashionsystem.fashion_system.util.DbTimes.toDb(day.atTime(8, 0)));
        AttendanceDtos.Excuse excuse = job(() -> excuses.create(staffA, new AttendanceDtos.ExcuseCreate(day, "FORGOT_CHECK_OUT",
                null, day.atTime(17, 30), "Quên bấm")));
        assertThat(excuse.status()).isEqualTo("PENDING");
        assertThatThrownBy(() -> job(() -> excuses.create(staffA, new AttendanceDtos.ExcuseCreate(day, "FORGOT_CHECK_OUT",
                null, day.atTime(17, 0), "Lần 2")))).hasMessageContaining("đã có đơn");

        // Quản lý cửa hàng khác không duyệt được; không ai tự duyệt đơn của mình.
        AttendanceAccess.Actor otherStore = new AttendanceAccess.Actor(UUID.randomUUID(), storeB, "B", true, true, true, true, false);
        assertThatThrownBy(() -> job(() -> excuses.approve(otherStore, excuse.id(), null))).isInstanceOf(BusinessException.class);
        AttendanceAccess.Actor self = new AttendanceAccess.Actor(staffA, storeA, "A", true, true, true, true, false);
        assertThatThrownBy(() -> job(() -> excuses.approve(self, excuse.id(), null))).hasMessageContaining("chính mình");

        job(() -> excuses.approve(storeManager, excuse.id(), new AttendanceDtos.Decision("OK")));
        UUID recordId = jdbc.queryForObject("select id from attendance_records where user_id = ? and attendance_date = ?", UUID.class, staffA, day);
        AttendanceDtos.RecordDetail detail = records.detail(storeManager, recordId);
        assertThat(detail.record().status()).isEqualTo("COMPLETED");
        assertThat(detail.record().earlyMinutes()).isEqualTo(30);
        assertThat(detail.record().source()).isEqualTo("EXCUSE");
        assertThat(detail.history()).singleElement().satisfies(h -> {
            assertThat(h.action()).isEqualTo("EXCUSE_APPROVED");
            assertThat(h.oldData()).contains("MISSING_CHECK_OUT");
            assertThat(h.newData()).contains("COMPLETED");
        });
    }

    @Test
    void leaveDaysSkipWeeklyOffAndHolidaysAndRespectQuota() {
        // Tuần 07–13/06/2027 (thứ Hai – Chủ nhật): CN không tính, 11/06 là ngày lễ, ngày đầu nghỉ nửa ngày.
        jdbc.update("delete from public_holidays where holiday_date between '2027-06-01' and '2027-06-30'");
        jdbc.update("insert into public_holidays(holiday_date, name) values ('2027-06-11', 'Ngày lễ thử')");
        UUID annual = jdbc.queryForObject("select id from leave_types where code = 'ANNUAL'", UUID.class);
        jdbc.update("update leave_types set annual_quota = 12 where id = ?", annual);
        jdbc.update("delete from leave_requests where user_id = ?", staffA);
        AttendanceDtos.Leave leave = job(() -> leaves.create(staffA, new AttendanceDtos.LeaveCreate(annual,
                LocalDate.of(2027, 6, 7), LocalDate.of(2027, 6, 13), true, false, "Về quê")));
        assertThat(leave.totalDays()).isEqualByComparingTo("4.5");           // 6 ngày − lễ 11/06 − nửa ngày đầu
        assertThatThrownBy(() -> job(() -> leaves.create(staffA, new AttendanceDtos.LeaveCreate(annual,
                LocalDate.of(2027, 6, 8), LocalDate.of(2027, 6, 8), false, false, "Trùng")))).hasMessageContaining("trùng");
        assertThatThrownBy(() -> job(() -> leaves.create(staffA, new AttendanceDtos.LeaveCreate(annual,
                LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 14), false, false, "Dài")))).hasMessageContaining("Vượt hạn mức");

        job(() -> leaves.approve(storeManager, leave.id(), null));
        AttendanceDtos.LeaveBalance balance = leaves.balance(staffA, 2027).stream().filter(b -> b.code().equals("ANNUAL")).findFirst().orElseThrow();
        assertThat(balance.used()).isEqualByComparingTo("4.5");
        assertThat(balance.remaining()).isEqualByComparingTo("7.5");

        AttendanceSummaryService.Summary summary = summaries.summarize(staffA, LocalDate.of(2027, 6, 7), LocalDate.of(2027, 6, 12));
        assertThat(summary.paidLeaveDays()).isEqualByComparingTo("4.5");
        assertThat(summary.holidayDays()).isEqualByComparingTo("1");
        assertThat(summary.unverifiedDays()).isEqualByComparingTo("0.5");   // nửa ngày 07/06 không có dữ liệu công
    }

    @Test
    void listsAreScopedToTheViewersStoreAndPendingRequestsAreNotCounted() {
        LocalDate day = LocalDate.now().minusDays(3);
        completed(staffA, storeA, day);
        completed(staffB, storeB, day);
        var filter = new AttendanceRecordService.Filter(day, day, null, null, null, "Zqa", null);
        assertThat(records.list(storeManager, filter, Pageable.ofSize(20)).getContent())
                .extracting(AttendanceDtos.Record::userId).containsExactly(staffA);
        assertThat(records.list(centralHr, filter, Pageable.ofSize(20)).getContent()).hasSize(2);
        assertThatThrownBy(() -> records.list(storeManager, new AttendanceRecordService.Filter(day, day, storeB, null, null, null, null),
                Pageable.ofSize(20))).hasMessageContaining("cửa hàng này");

        AttendanceDtos.Overtime ot = job(() -> overtimes.create(staffA, new AttendanceDtos.OvertimeCreate(day, new BigDecimal("2.5"), "Kiểm kho")));
        assertThat(summaries.summarize(staffA, day, day).otWeekdayHours().add(summaries.summarize(staffA, day, day).otWeekendHours()))
                .isEqualByComparingTo("0");
        job(() -> overtimes.approve(storeManager, ot.id(), new AttendanceDtos.OvertimeApprove(new BigDecimal("2"), null)));
        AttendanceSummaryService.Summary s = summaries.summarize(staffA, day, day);
        assertThat(s.otWeekdayHours().add(s.otWeekendHours()).add(s.otHolidayHours())).isEqualByComparingTo("2");
    }

    @Test
    void lockedMonthRejectsChanges() {
        LocalDate day = LocalDate.now().minusDays(1);
        completed(staffA, storeA, day);
        UUID recordId = jdbc.queryForObject("select id from attendance_records where user_id = ? and attendance_date = ?", UUID.class, staffA, day);
        UUID period = jdbc.queryForObject("""
                insert into payroll_periods(code, period_month, start_date, end_date, store_id, status)
                values (?, ?, ?, ?, ?, 'PENDING_APPROVAL') returning id
                """, UUID.class, "ZQA-" + tag, day.withDayOfMonth(1), day.withDayOfMonth(1), day.withDayOfMonth(day.lengthOfMonth()), storeA);
        jdbc.update("insert into attendance_locks(user_id, period_month, payroll_period_id) values (?, ?, ?)", staffA, day.withDayOfMonth(1), period);
        assertThatThrownBy(() -> job(() -> records.adjust(storeManager, recordId,
                new AttendanceDtos.AdjustRequest(day.atTime(8, 0), null, "Sửa")))).hasMessageContaining("đã chốt");
        assertThat(records.detail(storeManager, recordId).record().locked()).isTrue();
    }

    private void store(UUID id, String suffix, BigDecimal lat) {
        jdbc.update("insert into stores(id, code, name, active, latitude, longitude) values (?, ?, ?, true, ?, 106.700000)",
                id, "ZQA-" + suffix + tag, "Zqa Store " + suffix, lat);
    }

    private void user(UUID id, String name) {
        jdbc.update("insert into users(id, username, full_name, employee_code, active, employment_status) values (?, ?, ?, ?, true, 'ACTIVE')",
                id, "zqa-" + name + "-" + tag, "Zqa " + name, "ZQA" + name + tag);
    }

    private void staff(UUID user, UUID store) {
        jdbc.update("insert into store_staffs(user_id, store_id, active, start_date) values (?, ?, true, '2025-01-01')", user, store);
    }

    private void completed(UUID user, UUID store, LocalDate day) {
        LocalDateTime in = day.atTime(8, 0);
        jdbc.update("""
                insert into attendance_records(user_id, attendance_date, store_id, check_in_time, check_out_time, status,
                                               expected_start, expected_end, created_at)
                values (?, ?, ?, ?, ?, 'COMPLETED', '08:00', '18:00', now())
                """, user, day, store, com.fashionsystem.fashion_system.util.DbTimes.toDb(in),
                com.fashionsystem.fashion_system.util.DbTimes.toDb(day.atTime(18, 0)));
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_ATTENDANCE")) {
            return action.get();
        }
    }
}
