package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.helper.excel.ExcelAlignment;
import com.fashionsystem.fashion_system.helper.excel.ExcelCellFormat;
import com.fashionsystem.fashion_system.helper.excel.ExcelColumn;
import com.fashionsystem.fashion_system.helper.excel.ExcelHelper;
import com.fashionsystem.fashion_system.helper.excel.ExcelSheet;
import com.fashionsystem.fashion_system.helper.excel.ExcelWorkbookData;
import com.fashionsystem.fashion_system.payroll.PayrollAccess.Actor;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.CostRow;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Dashboard;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.EmployeeIssue;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodBrief;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodSummary;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard quỹ lương, báo cáo chi phí lương và xuất Excel, trong phạm vi người xem (không tính kỳ đã hủy). */
@Service
@Transactional(readOnly = true)
public class PayrollReportService {
    private static final Map<String, String> REVIEW = Map.of("OK", "Đủ dữ liệu", "NEEDS_REVIEW", "Cần xem xét", "CONFIRMED", "HR đã xác nhận");

    private final NamedParameterJdbcTemplate jdbc;
    private final PayrollPeriodService periods;
    private final ExcelHelper excel;

    public PayrollReportService(NamedParameterJdbcTemplate jdbc, PayrollPeriodService periods, ExcelHelper excel) {
        this.jdbc = jdbc;
        this.periods = periods;
        this.excel = excel;
    }

    private MapSqlParameterSource params(Actor actor, UUID storeId) {
        PayrollAccess.require(actor.permissions().view(), "Bạn không có quyền xem báo cáo lương");
        return PayrollAccess.params(actor).addValue("store", PayrollAccess.storeFilter(actor, storeId), java.sql.Types.OTHER)
;
    }

    /** Dòng lương còn hiệu lực trong phạm vi + bộ lọc cửa hàng. */
    private static final String RECORDS = """
            from payroll_records r join payroll_periods p on p.id = r.period_id
            where r.active and p.status <> 'CANCELLED' and (:global or p.store_id = :scopeStore)
              and (cast(:store as uuid) is null or p.store_id = :store)
            """;

    private static final String SUMS = """
            count(*) as employees, coalesce(sum(r.base_salary), 0) as base_salary, coalesce(sum(r.base_pay), 0) as base_pay,
            coalesce(sum(r.allowance_total), 0) as allowance, coalesce(sum(r.bonus_total), 0) as bonus,
            coalesce(sum(r.overtime_pay), 0) as overtime, coalesce(sum(r.deduction_total), 0) as deduction,
            coalesce(sum(r.gross_pay), 0) as gross, coalesce(sum(r.net_pay), 0) as net
            """;

    public Dashboard dashboard(Actor actor, String month, UUID storeId) {
        LocalDate m = (month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month)).atDay(1);
        MapSqlParameterSource p = params(actor, storeId).addValue("month", m).addValue("today", LocalDate.now());
        Map<String, Object> s = jdbc.queryForMap("select " + SUMS
                + ", count(*) filter (where r.review_status = 'NEEDS_REVIEW') as needs_review " + RECORDS + " and r.period_month = :month", p);
        List<PeriodBrief> list = jdbc.query("""
                select p.id, p.code, s.name as store_name, p.status,
                       (select count(*) from payroll_records r where r.period_id = p.id) as employees,
                       (select coalesce(sum(net_pay), 0) from payroll_records r where r.period_id = p.id) as net
                from payroll_periods p left join stores s on s.id = p.store_id
                where p.period_month = :month and (:global or p.store_id = :scopeStore)
                  and (cast(:store as uuid) is null or p.store_id = :store)
                order by s.name nulls first, p.created_at
                """, p, (rs, i) -> new PeriodBrief(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("store_name"),
                rs.getString("status"), rs.getInt("employees"), rs.getBigDecimal("net")));
        int pending = (int) list.stream().filter(b -> "PENDING_APPROVAL".equals(b.status())).count();
        int paid = (int) list.stream().filter(b -> "PAID".equals(b.status())).count();
        return new Dashboard(m, num(s, "employees"), dec(s, "gross"), dec(s, "base_salary"), dec(s, "base_pay"), dec(s, "allowance"),
                dec(s, "bonus"), dec(s, "overtime"), dec(s, "deduction"), dec(s, "net"), pending, paid, num(s, "needs_review"),
                missingSalary(actor, storeId).size(), list);
    }

    /** Chi phí lương theo tháng (12 tháng gần nhất tính tới {@code toMonth}) — dùng so sánh quỹ lương giữa các tháng. */
    public List<CostRow> byMonth(Actor actor, String toMonth, UUID storeId) {
        YearMonth end = toMonth == null || toMonth.isBlank() ? YearMonth.now() : YearMonth.parse(toMonth);
        MapSqlParameterSource p = params(actor, storeId).addValue("from", end.minusMonths(11).atDay(1)).addValue("to", end.atDay(1));
        return jdbc.query("select to_char(r.period_month, 'MM/YYYY') as label, " + SUMS + RECORDS
                + " and r.period_month between :from and :to group by r.period_month order by r.period_month", p, PayrollReportService::cost);
    }

    public List<CostRow> byStore(Actor actor, String month, UUID storeId) {
        MapSqlParameterSource p = params(actor, storeId).addValue("month", YearMonth.parse(month).atDay(1));
        return jdbc.query("select coalesce(r.store_name, 'Văn phòng (Global)') as label, " + SUMS + RECORDS
                + " and r.period_month = :month group by 1 order by 1", p, PayrollReportService::cost);
    }

    public List<CostRow> byDepartment(Actor actor, String month, UUID storeId) {
        MapSqlParameterSource p = params(actor, storeId).addValue("month", YearMonth.parse(month).atDay(1));
        return jdbc.query("select coalesce(r.department_name, 'Chưa gán phòng ban') as label, " + SUMS + RECORDS
                + " and r.period_month = :month group by 1 order by 1", p, PayrollReportService::cost);
    }

    /** Nhân viên đang làm việc chưa có hồ sơ lương hiệu lực hôm nay. */
    public List<EmployeeIssue> missingSalary(Actor actor, UUID storeId) {
        MapSqlParameterSource p = params(actor, storeId).addValue("today", LocalDate.now());
        return jdbc.query("""
                select u.id, u.employee_code, u.full_name, st.name as store_name from users u
                left join store_staffs ss on ss.user_id = u.id and ss.active
                left join stores st on st.id = ss.store_id
                where %s and u.active and %s
                  and (cast(:store as uuid) is null or ss.store_id = :store)
                  and not exists (select 1 from salary_structures s where s.user_id = u.id and s.effective_from <= :today
                                  and (s.effective_to is null or s.effective_to >= :today))
                order by u.full_name
                """.formatted(SalaryStructureService.EMPLOYEE, PayrollAccess.employeeGuard("u")), p,
                (rs, i) -> new EmployeeIssue(rs.getObject("id", UUID.class), rs.getString("employee_code"), rs.getString("full_name"),
                        rs.getString("store_name"), "Chưa có hồ sơ lương"));
    }

    /** Nhân viên trong bảng lương tháng còn thiếu dữ liệu tính lương. */
    public List<EmployeeIssue> incomplete(Actor actor, String month, UUID storeId) {
        MapSqlParameterSource p = params(actor, storeId).addValue("month", YearMonth.parse(month).atDay(1));
        return jdbc.query("select r.user_id, r.employee_code, r.full_name, r.store_name, r.unverified_days, r.confirmed_absent_days, "
                + "r.pending_request_count, r.missing_check_out_count, r.missing_check_in_count, r.review_status, r.calculated_at " + RECORDS
                + " and r.period_month = :month and (r.review_status = 'NEEDS_REVIEW' or r.calculated_at is null) order by r.full_name", p,
                (rs, i) -> {
                    List<String> issues = new ArrayList<>();
                    if (rs.getBigDecimal("unverified_days").signum() > 0 && !"CONFIRMED".equals(rs.getString("review_status")))
                        issues.add(rs.getBigDecimal("unverified_days").stripTrailingZeros().toPlainString() + " ngày công chưa xác minh");
                    if (rs.getInt("pending_request_count") > 0) issues.add(rs.getInt("pending_request_count") + " đơn chờ duyệt");
                    int missing = rs.getInt("missing_check_out_count") + rs.getInt("missing_check_in_count");
                    if (missing > 0) issues.add(missing + " lần thiếu chấm công");
                    if (rs.getTimestamp("calculated_at") == null) issues.add("chưa tính lương");
                    if (issues.isEmpty()) issues.add("thực nhận âm");
                    return new EmployeeIssue(rs.getObject("user_id", UUID.class), rs.getString("employee_code"), rs.getString("full_name"),
                            rs.getString("store_name"), String.join(", ", issues));
                });
    }

    // ------------------------------------------------------------------ Excel

    public byte[] exportPeriod(Actor actor, UUID periodId) {
        PayrollAccess.require(actor.permissions().export(), "Bạn không có quyền xuất bảng lương");
        PeriodSummary period = periods.summary(periodId);
        List<RecordRow> rows = periods.records(actor, periodId, null, null, org.springframework.data.domain.Pageable.ofSize(5000)).getContent();
        List<ExcelColumn<RecordRow>> columns = List.of(
                text("Mã NV", RecordRow::employeeCode), text("Họ tên", RecordRow::fullName),
                text("Cửa hàng / phòng ban", r -> (r.storeName() == null ? "Văn phòng" : r.storeName()) + (r.departmentName() == null ? "" : " · " + r.departmentName())),
                text("Chức vụ", RecordRow::positionName), money("Lương cơ bản", RecordRow::baseSalary),
                number("Ngày công chuẩn", RecordRow::standardDays), number("Ngày được trả lương", RecordRow::payableDays),
                number("Ngày đi làm", RecordRow::workedDays), number("Nghỉ có lương", RecordRow::paidLeaveDays),
                number("Nghỉ không lương", r -> r.unpaidLeaveDays().add(r.confirmedAbsentDays())),
                money("Lương theo công", RecordRow::basePay), money("Phụ cấp", RecordRow::allowanceTotal),
                money("Thưởng", RecordRow::bonusTotal), money("Tăng ca", RecordRow::overtimePay),
                money("Khấu trừ", RecordRow::deductionTotal), money("Tổng thu nhập", RecordRow::grossPay),
                money("Thực nhận", RecordRow::netPay), text("Dữ liệu", r -> REVIEW.getOrDefault(r.reviewStatus(), r.reviewStatus())),
                text("Thanh toán", r -> "PAID".equals(r.paymentStatus()) ? "Đã trả" : "Chưa trả"));
        return excel.export(new ExcelWorkbookData(List.of(new ExcelSheet<>("Bang luong " + period.periodMonth().toString().substring(0, 7), columns, rows))));
    }

    public byte[] exportReports(Actor actor, String month, UUID storeId) {
        PayrollAccess.require(actor.permissions().export(), "Bạn không có quyền xuất báo cáo lương");
        List<ExcelColumn<CostRow>> cost = List.of(text("Nhóm", CostRow::label), number("Số nhân viên", CostRow::employeeCount),
                money("Lương theo công", CostRow::totalBasePay), money("Phụ cấp", CostRow::totalAllowance), money("Thưởng", CostRow::totalBonus),
                money("Tăng ca", CostRow::totalOvertime), money("Khấu trừ", CostRow::totalDeduction),
                money("Tổng quỹ lương", CostRow::totalGross), money("Thực nhận", CostRow::totalNet));
        List<ExcelColumn<EmployeeIssue>> issue = List.of(text("Mã NV", EmployeeIssue::employeeCode), text("Họ tên", EmployeeIssue::fullName),
                text("Cửa hàng", EmployeeIssue::storeName), text("Vấn đề", EmployeeIssue::issue));
        return excel.export(new ExcelWorkbookData(List.of(
                new ExcelSheet<>("Theo tháng", cost, byMonth(actor, month, storeId)),
                new ExcelSheet<>("Theo cửa hàng " + month, cost, byStore(actor, month, storeId)),
                new ExcelSheet<>("Theo phòng ban " + month, cost, byDepartment(actor, month, storeId)),
                new ExcelSheet<>("Chưa có hồ sơ lương", issue, missingSalary(actor, storeId)),
                new ExcelSheet<>("Thiếu dữ liệu " + month, issue, incomplete(actor, month, storeId)))));
    }

    private static CostRow cost(ResultSet rs, int i) throws SQLException {
        return new CostRow(rs.getString("label"), rs.getInt("employees"), rs.getBigDecimal("base_pay"), rs.getBigDecimal("allowance"),
                rs.getBigDecimal("bonus"), rs.getBigDecimal("overtime"), rs.getBigDecimal("deduction"), rs.getBigDecimal("gross"),
                rs.getBigDecimal("net"));
    }

    private static int num(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).intValue();
    }

    private static java.math.BigDecimal dec(Map<String, Object> row, String key) {
        return (java.math.BigDecimal) row.get(key);
    }

    private static <T> ExcelColumn<T> text(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, value);
    }

    private static <T> ExcelColumn<T> number(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, value, ExcelCellFormat.NUMBER, null, ExcelAlignment.RIGHT);
    }

    private static <T> ExcelColumn<T> money(String header, Function<T, ?> value) {
        return new ExcelColumn<>(header, value, ExcelCellFormat.CURRENCY, "#,##0", ExcelAlignment.RIGHT);
    }
}
