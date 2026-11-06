package com.fashionsystem.fashion_system.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos;
import com.fashionsystem.fashion_system.attendance.AttendanceRecordService;
import com.fashionsystem.fashion_system.attendance.ExcuseRequestService;
import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordRow;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính lương trên cơ sở dữ liệu thật (rollback sau mỗi test). Tháng 03/2026: 31 ngày, 5 Chủ nhật, không có ngày lễ →
 * 26 ngày công chuẩn theo lịch.
 */
@SpringBootTest
@Transactional
class PayrollIntegrationTest {
    @Autowired PayrollPeriodService periods;
    @Autowired PayrollRecordService records;
    @Autowired SalaryStructureService salaries;
    @Autowired PayslipService payslips;
    @Autowired PayrollReportService reports;
    @Autowired AttendanceRecordService attendance;
    @Autowired ExcuseRequestService excuses;
    @Autowired JdbcTemplate jdbc;

    final String tag = UUID.randomUUID().toString().substring(0, 6);
    final UUID storeA = UUID.randomUUID();
    final UUID storeB = UUID.randomUUID();
    final UUID hrGlobal = UUID.randomUUID();
    final UUID hrStore = UUID.randomUUID();
    final UUID emp1 = UUID.randomUUID();
    final UUID emp2 = UUID.randomUUID();
    final UUID emp3 = UUID.randomUUID();
    final UUID noSalary = UUID.randomUUID();
    PayrollAccess.Actor global;
    PayrollAccess.Actor storeHr;

    @BeforeEach
    void setUp() {
        jdbc.update("update payroll_settings set standard_days_mode = 'CALENDAR', rounding_unit = 1000, rounding_mode = 'HALF_UP', "
                + "ot_weekday_rate = 1.5, ot_weekend_rate = 2, ot_holiday_rate = 3 where id = 1");
        jdbc.update("update attendance_settings set weekly_off_days = '{7}', standard_hours_per_day = 8 where id = 1");
        jdbc.update("delete from public_holidays where holiday_date between '2026-03-01' and '2026-04-30' and holiday_date <> '2026-04-30'");
        store(storeA, "A");
        store(storeB, "B");
        user(hrGlobal, "hrg", null);
        user(hrStore, "hrs", null);
        user(emp1, "e1", LocalDate.of(2025, 1, 1));
        user(emp2, "e2", LocalDate.of(2026, 3, 16));
        user(emp3, "e3", LocalDate.of(2025, 1, 1));
        user(noSalary, "ns", LocalDate.of(2025, 1, 1));
        staff(hrStore, storeA);
        staff(emp1, storeA);
        staff(emp2, storeA);
        staff(noSalary, storeA);
        staff(emp3, storeB);
        global = actor(hrGlobal, null);
        storeHr = actor(hrStore, storeA);

        UUID meal = component("MEAL");
        UUID bonus = component("ATTENDANCE_BONUS");
        UUID late = component("LATE_PENALTY");
        salaries.create(global, emp1, structure("10400000", "2026-01-01", List.of(
                new PayrollDtos.StructureItemRequest(meal, new BigDecimal("30000"), null, null),
                new PayrollDtos.StructureItemRequest(bonus, new BigDecimal("300000"), null, null),
                new PayrollDtos.StructureItemRequest(late, new BigDecimal("20000"), null, null))));
        salaries.create(global, emp2, structure("7800000", "2026-03-16", List.of()));
        salaries.create(global, emp3, structure("9000000", "2026-01-01", List.of()));

        // emp1: đi làm đủ mọi ngày làm việc tháng 3, trừ 10–11/03 nghỉ không lương (đã duyệt); 05/03 đi trễ 20 phút.
        for (LocalDate d = LocalDate.of(2026, 3, 1); !d.isAfter(LocalDate.of(2026, 3, 31)); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == java.time.DayOfWeek.SUNDAY || d.getDayOfMonth() == 10 || d.getDayOfMonth() == 11) continue;
            completed(emp1, storeA, d, d.getDayOfMonth() == 5 ? 20 : 0);
        }
        UUID unpaid = jdbc.queryForObject("select id from leave_types where code = 'UNPAID'", UUID.class);
        jdbc.update("""
                insert into leave_requests(code, user_id, leave_type_id, start_date, end_date, is_half_day_start, is_half_day_end,
                                           store_id, status, total_days, created_at)
                values (?, ?, ?, '2026-03-10', '2026-03-11', false, false, ?, 'APPROVED', 2, now())
                """, "NP-T" + tag, emp1, unpaid, storeA);
        jdbc.update("""
                insert into overtime_requests(code, user_id, store_id, work_date, hours, approved_hours, overtime_type, reason, status, created_at)
                values (?, ?, ?, '2026-03-12', 4, 4, 'WEEKDAY', 'Kiểm kê', 'APPROVED', now())
                """, "TC-T" + tag, emp1, storeA);
    }

    @Test
    void calculatesFixedSalaryFromProcessedAttendance() {
        PeriodDetail period = job(() -> periods.create(global, new PayrollDtos.PeriodCreate("2026-03", storeA, null)));
        PayrollDtos.SyncResult sync = job(() -> periods.sync(global, period.period().id()));
        assertThat(sync.added()).isEqualTo(2);
        assertThat(sync.skipped()).extracting(PayrollDtos.SkippedEmployee::userId).contains(noSalary);
        job(() -> periods.calculate(global, period.period().id()));

        RecordRow e1 = row(period.period().id(), emp1);
        assertThat(e1.standardDays()).isEqualByComparingTo("26");
        assertThat(e1.unpaidLeaveDays()).isEqualByComparingTo("2");
        assertThat(e1.payableDays()).isEqualByComparingTo("24");
        assertThat(e1.basePay()).isEqualByComparingTo("9600000");          // 10.4tr × 24/26
        assertThat(e1.allowanceTotal()).isEqualByComparingTo("720000");    // ăn trưa 24 ngày đi làm
        assertThat(e1.bonusTotal()).isZero();                               // không đạt chuyên cần
        assertThat(e1.overtimePay()).isEqualByComparingTo("300000");       // 50.000 × 4 giờ × 1.5
        assertThat(e1.deductionTotal()).isEqualByComparingTo("20000");     // 1 lần đi trễ
        assertThat(e1.netPay()).isEqualByComparingTo("10600000");
        assertThat(e1.reviewStatus()).isEqualTo("OK");

        // Vào làm 16/03, chưa có dữ liệu chấm công: không tự trừ lương, nhưng cần HR xác minh.
        RecordRow e2 = row(period.period().id(), emp2);
        assertThat(e2.unverifiedDays()).isEqualByComparingTo("14");
        assertThat(e2.basePay()).isEqualByComparingTo("4200000");          // 7.8tr × 14/26
        assertThat(e2.reviewStatus()).isEqualTo("NEEDS_REVIEW");
        assertThat(periods.records(global, period.period().id(), null, null, Pageable.ofSize(50)).getContent())
                .extracting(RecordRow::userId).doesNotContain(emp3);
    }

    @Test
    void unverifiedDaysAndPendingRequestsBlockSubmitUntilResolved() {
        UUID id = calculatedPeriod();
        assertThatThrownBy(() -> job(() -> periods.submit(global, id, null))).hasMessageContaining("cần xem xét");

        UUID r2 = row(id, emp2).id();
        job(() -> records.confirmDays(global, r2, new PayrollDtos.ConfirmDays(new BigDecimal("2"), "Nghỉ không phép 2 ngày")));
        assertThat(row(id, emp2).basePay()).isEqualByComparingTo("3600000");   // 7.8tr × 12/26
        assertThat(jdbc.queryForObject("select count(*) from payroll_adjustments where record_id = ? and action = 'CONFIRM_DAYS'",
                Integer.class, r2)).isEqualTo(1);

        // Đơn giải trình đang chờ không được tính vào công, và chặn gửi duyệt sau khi đồng bộ lại.
        jdbc.update("""
                insert into excuse_requests(code, user_id, attendance_date, excuse_type, proposed_check_out, reason, status, created_at)
                values (?, ?, '2026-03-09', 'FORGOT_CHECK_OUT', ?, 'Quên', 'PENDING', now())
                """, "GT-T" + tag, emp1, DbTimes.toDb(LocalDateTime.of(2026, 3, 9, 17, 30)));
        job(() -> periods.sync(global, id));
        job(() -> periods.calculate(global, id));
        assertThat(row(id, emp1).reviewStatus()).isEqualTo("NEEDS_REVIEW");
        assertThatThrownBy(() -> job(() -> records.confirmDays(global, row(id, emp1).id(),
                new PayrollDtos.ConfirmDays(BigDecimal.ZERO, "x")))).hasMessageContaining("đơn chờ duyệt");
        assertThat(row(id, emp1).payableDays()).isEqualByComparingTo("24");
    }

    @Test
    void approvalLocksAttendanceAndPublishesPayslipsOnlyToTheirOwner() {
        UUID id = readyPeriod();
        UUID r1 = row(id, emp1).id();
        assertThat(payslips.mine(emp1)).isEmpty();                              // chưa phát hành

        job(() -> periods.submit(global, id, "Gửi duyệt"));
        AttendanceAccess.Actor attendanceHr = new AttendanceAccess.Actor(hrGlobal, null, null, true, true, true, true, true);
        UUID march5 = jdbc.queryForObject("select id from attendance_records where user_id = ? and attendance_date = '2026-03-05'", UUID.class, emp1);
        assertThatThrownBy(() -> job(() -> attendance.adjust(attendanceHr, march5,
                new AttendanceDtos.AdjustRequest(LocalDateTime.of(2026, 3, 5, 8, 0), null, "Sửa"))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã chốt");
        assertThatThrownBy(() -> job(() -> records.addItem(global, r1,
                new PayrollDtos.ItemRequest(null, "Thưởng", "BONUS", BigDecimal.TEN, null, "x")))).hasMessageContaining("chờ duyệt");

        job(() -> periods.approve(global, id, null));
        assertThat(payslips.mine(emp1)).extracting(RecordRow::id).containsExactly(r1);
        assertThat(payslips.mineDetail(emp1, r1).row().netPay()).isEqualByComparingTo("10600000");
        assertThatThrownBy(() -> payslips.mineDetail(emp3, r1)).hasMessageContaining("không tồn tại");
        assertThat(payslips.minePdf(emp1, r1)).startsWith("%PDF".getBytes());

        // Lương đã chốt: không đổi hồ sơ lương hiệu lực trong tháng đó.
        assertThatThrownBy(() -> job(() -> salaries.create(global, emp1, structure("11000000", "2026-03-20", List.of()))))
                .hasMessageContaining("đã chốt");

        PayrollDtos.PaymentResult paid = job(() -> periods.pay(global, id, new PayrollDtos.PaymentRequest(List.of(r1), "UNC-01", null, null)));
        assertThat(paid.paid()).isEqualTo(1);
        assertThat(paid.periodStatus()).isEqualTo("APPROVED");
        assertThatThrownBy(() -> job(() -> periods.pay(global, id, new PayrollDtos.PaymentRequest(List.of(r1), null, null, null))))
                .hasMessageContaining("đã được thanh toán");
        assertThat(job(() -> periods.pay(global, id, null)).periodStatus()).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("select count(*) from payroll_payments where record_id = ?", Integer.class, r1)).isEqualTo(1);
        assertThatThrownBy(() -> job(() -> periods.cancel(global, id, "x"))).hasMessageContaining("đã chốt");
        assertThat(jdbc.queryForList("select action from payroll_approvals where period_id = ? order by created_at", String.class, id))
                .contains("CREATE", "SYNC_ATTENDANCE", "CALCULATE", "SUBMIT", "APPROVE", "PAY");
    }

    @Test
    void rejectionUnlocksAttendanceAndCancelFreesEmployeesForAnotherPeriod() {
        UUID id = readyPeriod();
        job(() -> periods.submit(global, id, null));
        assertThat(jdbc.queryForObject("select count(*) from attendance_locks where payroll_period_id = ?", Integer.class, id)).isEqualTo(2);
        PeriodDetail rejected = job(() -> periods.reject(global, id, "Sai phụ cấp"));
        assertThat(rejected.period().status()).isEqualTo("CALCULATED");
        assertThat(rejected.period().lastRejectionReason()).isEqualTo("Sai phụ cấp");
        assertThat(jdbc.queryForObject("select count(*) from attendance_locks where payroll_period_id = ?", Integer.class, id)).isZero();

        assertThatThrownBy(() -> job(() -> periods.create(global, new PayrollDtos.PeriodCreate("2026-03", storeA, null))))
                .hasMessageContaining("Đã có kỳ lương");
        job(() -> periods.cancel(global, id, "Lập lại"));
        PeriodDetail again = job(() -> periods.create(global, new PayrollDtos.PeriodCreate("2026-03", storeA, null)));
        assertThat(job(() -> periods.sync(global, again.period().id())).added()).isEqualTo(2);
    }

    @Test
    void afterCloseAdjustmentIsCarriedIntoTheNextPeriod() {
        UUID march = readyPeriod();
        job(() -> periods.submit(global, march, null));
        job(() -> periods.approve(global, march, null));
        UUID r1 = row(march, emp1).id();
        assertThatThrownBy(() -> job(() -> records.addItem(global, r1,
                new PayrollDtos.ItemRequest(null, "Thưởng", "BONUS", BigDecimal.TEN, null, "x")))).isInstanceOf(BusinessException.class);
        job(() -> records.afterClose(global, r1, new PayrollDtos.AfterCloseAdjustment("BONUS", "Truy lĩnh thưởng T3",
                new BigDecimal("500000"), "Bổ sung theo quyết định")));
        assertThat(row(march, emp1).netPay()).isEqualByComparingTo("10600000");   // bảng lương đã chốt giữ nguyên

        PeriodDetail april = job(() -> periods.create(global, new PayrollDtos.PeriodCreate("2026-04", storeA, null)));
        job(() -> periods.sync(global, april.period().id()));
        job(() -> periods.calculate(global, april.period().id()));
        var detail = records.detail(global, row(april.period().id(), emp1).id());
        assertThat(detail.items()).anySatisfy(i -> {
            assertThat(i.source()).isEqualTo("ADJUSTMENT");
            assertThat(i.amount()).isEqualByComparingTo("500000");
        });
        assertThat(jdbc.queryForObject("select status from payroll_adjustments where record_id = ? and phase = 'AFTER_CLOSE'",
                String.class, r1)).isEqualTo("PENDING");
    }

    @Test
    void storeHrOnlySeesOwnStoreAndConfidentialSalariesAreHidden() {
        UUID id = calculatedPeriod();
        PeriodDetail b = job(() -> periods.create(global, new PayrollDtos.PeriodCreate("2026-03", storeB, null)));
        assertThatThrownBy(() -> periods.detail(storeHr, b.period().id())).hasMessageContaining("phạm vi");
        assertThatThrownBy(() -> job(() -> periods.create(storeHr, new PayrollDtos.PeriodCreate("2026-03", storeB, null))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> job(() -> periods.create(storeHr, new PayrollDtos.PeriodCreate("2026-03", null, null))))
                .hasMessageContaining("cửa hàng của mình");
        assertThat(periods.list(storeHr, "2026-03", null, null, Pageable.ofSize(20)).getContent())
                .extracting(PayrollDtos.PeriodSummary::id).containsExactly(id);
        assertThat(salaries.list(storeHr, null, null, null, Pageable.ofSize(50)).getContent())
                .extracting(PayrollDtos.EmployeeSalary::userId).contains(emp1).doesNotContain(emp3);
        assertThatThrownBy(() -> salaries.detail(storeHr, emp3)).isInstanceOf(BusinessException.class);

        jdbc.update("update salary_structures set confidential = true where user_id = ?", emp1);
        UUID r1 = row(id, emp1).id();
        assertThatThrownBy(() -> records.detail(storeHr, r1)).hasMessageContaining("bảo mật");
        assertThat(periods.records(storeHr, id, null, null, Pageable.ofSize(50)).getContent())
                .extracting(RecordRow::userId).containsExactly(emp2);
        assertThat(records.detail(actor(hrGlobal, null), r1).row().userId()).isEqualTo(emp1);
    }

    @Test
    void salaryChangesCreateVersionsWithHistoryAndCannotBeSelfEdited() {
        job(() -> salaries.create(global, emp1, structure("11440000", "2026-05-01", List.of())));
        var detail = salaries.detail(global, emp1);
        assertThat(detail.structures()).extracting(PayrollDtos.Structure::baseSalary)
                .usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("11440000"), new BigDecimal("10400000"));
        assertThat(detail.structures().get(1).effectiveTo()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(detail.history()).extracting(PayrollDtos.HistoryEntry::action).contains("CREATE", "CLOSE", "NEW_VERSION");
        assertThatThrownBy(() -> job(() -> salaries.create(global, emp1, structure("12000000", "2026-04-01", List.of()))))
                .hasMessageContaining("sau ngày đó");

        PayrollAccess.Actor self = actor(emp1, storeA);
        assertThatThrownBy(() -> job(() -> salaries.create(self, emp1, structure("99000000", "2026-06-01", List.of()))))
                .hasMessageContaining("chính mình");
    }

    @Test
    void reportsSummariseCostAndFlagMissingData() {
        calculatedPeriod();
        var byStore = reports.byStore(global, "2026-03", storeA);
        assertThat(byStore).singleElement().satisfies(r -> {
            assertThat(r.employeeCount()).isEqualTo(2);
            assertThat(r.totalNet()).isEqualByComparingTo("14800000");   // 10.6tr + 4.2tr
        });
        assertThat(reports.missingSalary(global, storeA)).extracting(PayrollDtos.EmployeeIssue::userId).contains(noSalary);
        assertThat(reports.incomplete(global, "2026-03", storeA)).extracting(PayrollDtos.EmployeeIssue::userId).containsExactly(emp2);
        assertThat(reports.exportReports(global, "2026-03", storeA)).isNotEmpty();
    }

    // ------------------------------------------------------------------ fixture

    private UUID calculatedPeriod() {
        PeriodDetail period = job(() -> periods.create(global, new PayrollDtos.PeriodCreate("2026-03", storeA, null)));
        job(() -> periods.sync(global, period.period().id()));
        job(() -> periods.calculate(global, period.period().id()));
        return period.period().id();
    }

    /** Kỳ đã tính và HR đã xác minh ngày công của emp2 → gửi duyệt được. */
    private UUID readyPeriod() {
        UUID id = calculatedPeriod();
        job(() -> records.confirmDays(global, row(id, emp2).id(), new PayrollDtos.ConfirmDays(BigDecimal.ZERO, "Đã xác minh đi làm đủ")));
        return id;
    }

    private RecordRow row(UUID periodId, UUID userId) {
        return periods.records(global, periodId, null, null, Pageable.ofSize(50)).getContent().stream()
                .filter(r -> r.userId().equals(userId)).findFirst().orElseThrow();
    }

    private PayrollAccess.Actor actor(UUID userId, UUID storeId) {
        return new PayrollAccess.Actor(userId, storeId, storeId == null ? null : "Store", true, true, true, true, true, true, true, storeId == null);
    }

    private PayrollDtos.StructureRequest structure(String base, String from, List<PayrollDtos.StructureItemRequest> items) {
        return new PayrollDtos.StructureRequest(new BigDecimal(base), LocalDate.parse(from), null, "CASH", null, null, true, null, false,
                null, "Ký hợp đồng", items);
    }

    private UUID component(String code) {
        return jdbc.queryForObject("select id from payroll_components where code = ?", UUID.class, code);
    }

    private void store(UUID id, String suffix) {
        jdbc.update("insert into stores(id, code, name, active) values (?, ?, ?, true)", id, "ZQP-" + suffix + tag, "Zqp Store " + suffix);
    }

    private void user(UUID id, String name, LocalDate hire) {
        jdbc.update("insert into users(id, username, full_name, employee_code, hire_date, active, employment_status) values (?, ?, ?, ?, ?, true, 'ACTIVE')",
                id, "zqp-" + name + "-" + tag + id.toString().substring(0, 4), "Zqp " + name, "ZQP" + name + tag + id.toString().substring(0, 3), hire);
    }

    private void staff(UUID user, UUID store) {
        jdbc.update("insert into store_staffs(user_id, store_id, active, start_date) values (?, ?, true, '2025-01-01')", user, store);
    }

    private void completed(UUID user, UUID store, LocalDate day, int late) {
        jdbc.update("""
                insert into attendance_records(user_id, attendance_date, store_id, check_in_time, check_out_time, status, late_minutes,
                                               expected_start, expected_end, source, created_at)
                values (?, ?, ?, ?, ?, 'COMPLETED', ?, '08:00', '18:00', 'SELF', now())
                """, user, day, store, DbTimes.toDb(day.atTime(8, late)), DbTimes.toDb(day.atTime(18, 0)), late);
    }

    private <T> T job(Supplier<T> action) {
        try (var ignored = BusinessAuditContextFactory.systemJob("TEST_PAYROLL")) {
            return action.get();
        }
    }
}
