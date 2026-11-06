package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.attendance.AttendancePolicy;
import com.fashionsystem.fashion_system.attendance.AttendanceSummaryService;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.SkippedEmployee;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.StructureItem;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.SyncResult;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Đồng bộ dữ liệu công vào bảng lương và tính lương từng nhân viên. Không tự kiểm tra quyền / trạng thái — các service
 * gọi đã kiểm tra.
 */
@Component
class PayrollEngine {
    static final String SOURCE = "ATTENDANCE_V1";

    private final NamedParameterJdbcTemplate jdbc;
    private final AttendanceSummaryService attendance;
    private final AttendancePolicy attendancePolicy;
    private final SalaryStructureService structures;
    private final PayrollSettingsService settings;
    private final ObjectMapper json;

    PayrollEngine(NamedParameterJdbcTemplate jdbc, AttendanceSummaryService attendance, AttendancePolicy attendancePolicy,
                  SalaryStructureService structures, PayrollSettingsService settings, ObjectMapper json) {
        this.jdbc = jdbc;
        this.attendance = attendance;
        this.attendancePolicy = attendancePolicy;
        this.structures = structures;
        this.settings = settings;
        this.json = json;
    }

    /** Thông tin kỳ lương cần cho đồng bộ / tính. */
    record Period(UUID id, String code, LocalDate month, LocalDate start, LocalDate end, UUID storeId, String status,
                  BigDecimal standardDays, JsonNode settingsSnapshot, LocalDateTime syncedAt, LocalDateTime calculatedAt,
                  UUID submittedBy) {
    }

    Period period(UUID id, boolean forUpdate) {
        List<Period> rows = jdbc.query("select * from payroll_periods where id = :id" + (forUpdate ? " for update" : ""),
                new MapSqlParameterSource("id", id), (rs, i) -> new Period(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getObject("period_month", LocalDate.class), rs.getObject("start_date", LocalDate.class),
                        rs.getObject("end_date", LocalDate.class), rs.getObject("store_id", UUID.class), rs.getString("status"),
                        rs.getBigDecimal("standard_days"), parse(rs.getString("settings_snapshot")),
                        DbTimes.fromDb(rs.getTimestamp("attendance_synced_at")), DbTimes.fromDb(rs.getTimestamp("calculated_at")),
                        rs.getObject("submitted_by", UUID.class)));
        if (rows.isEmpty()) throw com.fashionsystem.fashion_system.exception.BusinessException.notFound("Kỳ lương không tồn tại");
        return rows.getFirst();
    }

    // ------------------------------------------------------------------ đồng bộ công

    private record Candidate(UUID userId, String code, String name, LocalDate hire, LocalDate termination) {
    }

    SyncResult sync(Period period, UUID actorId) {
        MapSqlParameterSource p = new MapSqlParameterSource("start", period.start()).addValue("end", period.end())
                .addValue("store", period.storeId(), java.sql.Types.OTHER).addValue("headOffice", period.storeId() == null)
                .addValue("month", period.month()).addValue("period", period.id());
        String overlap = "(ss.active or (ss.end_date is not null and ss.end_date >= :start)) and (ss.start_date is null or ss.start_date <= :end)";
        List<Candidate> candidates = jdbc.query("""
                select u.id, u.employee_code, u.full_name, u.hire_date, u.termination_date from users u
                where %s and (u.active or u.termination_date is not null)
                  and (u.hire_date is null or u.hire_date <= :end) and (u.termination_date is null or u.termination_date >= :start)
                  and ((not :headOffice and exists (select 1 from store_staffs ss where ss.user_id = u.id and ss.store_id = :store and %s))
                       or (:headOffice and not exists (select 1 from store_staffs ss where ss.user_id = u.id and %s)))
                order by u.full_name
                """.formatted(SalaryStructureService.EMPLOYEE, overlap, overlap), p,
                (rs, i) -> new Candidate(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                        rs.getObject(4, LocalDate.class), rs.getObject(5, LocalDate.class)));

        List<SkippedEmployee> skipped = new ArrayList<>();
        Set<UUID> kept = new HashSet<>();
        int added = 0, updated = 0;
        for (Candidate c : candidates) {
            LocalDate ws = c.hire() != null && c.hire().isAfter(period.start()) ? c.hire() : period.start();
            LocalDate we = c.termination() != null && c.termination().isBefore(period.end()) ? c.termination() : period.end();
            if (we.isBefore(ws)) continue;
            List<SalaryStructureService.Version> versions = structures.versions(c.userId(), ws, we);
            if (versions.isEmpty()) {
                skipped.add(new SkippedEmployee(c.userId(), c.code(), c.name(), "Chưa có hồ sơ lương hiệu lực trong kỳ"));
                continue;
            }
            List<String> other = jdbc.queryForList("""
                    select p.code from payroll_records r join payroll_periods p on p.id = r.period_id
                    where r.user_id = :u and r.period_month = :month and r.active and r.period_id <> :period
                    """, new MapSqlParameterSource("u", c.userId()).addValue("month", period.month()).addValue("period", period.id()), String.class);
            if (!other.isEmpty()) {
                skipped.add(new SkippedEmployee(c.userId(), c.code(), c.name(), "Đã thuộc bảng lương " + other.getFirst()));
                continue;
            }
            AttendanceSummaryService.Summary s = attendance.summarize(c.userId(), ws, we);
            Map<String, Object> info = jdbc.queryForMap("""
                    select (select st.name from store_staffs ss join stores st on st.id = ss.store_id where ss.user_id = u.id
                            order by ss.active desc, ss.created_at desc limit 1) as store_name,
                           (select ss.store_id from store_staffs ss where ss.user_id = u.id order by ss.active desc, ss.created_at desc limit 1) as store_id,
                           (select string_agg(d.name, ', ' order by d.name) from user_departments ud join departments d on d.id = ud.department_id
                            where ud.user_id = u.id) as department_name,
                           pos.name as position_name
                    from users u left join positions pos on pos.id = u.position_id where u.id = :u
                    """, new MapSqlParameterSource("u", c.userId()));
            boolean storeEmployee = period.storeId() != null;
            MapSqlParameterSource r = new MapSqlParameterSource("period", period.id()).addValue("u", c.userId())
                    .addValue("month", period.month()).addValue("code", c.code()).addValue("name", c.name())
                    .addValue("type", storeEmployee ? "STORE" : "GLOBAL")
                    .addValue("storeId", storeEmployee ? period.storeId() : null, java.sql.Types.OTHER)
                    .addValue("storeName", storeEmployee ? info.get("store_name") : null)
                    .addValue("dept", info.get("department_name")).addValue("pos", info.get("position_name"))
                    .addValue("ws", ws).addValue("we", we).addValue("workdays", s.workdays()).addValue("worked", s.workedDays())
                    .addValue("offWorked", s.offDayWorkedDays()).addValue("minutes", s.workedMinutes()).addValue("paidLeave", s.paidLeaveDays())
                    .addValue("unpaidLeave", s.unpaidLeaveDays()).addValue("holidays", s.holidayDays()).addValue("unverified", s.unverifiedDays())
                    .addValue("late", s.lateCount()).addValue("early", s.earlyCount()).addValue("lateMin", s.lateMinutes())
                    .addValue("earlyMin", s.earlyMinutes()).addValue("missOut", s.missingCheckOut()).addValue("missIn", s.missingCheckIn())
                    .addValue("excuses", s.approvedExcuses()).addValue("pending", s.pendingRequests())
                    .addValue("otWd", s.otWeekdayHours()).addValue("otWe", s.otWeekendHours()).addValue("otHol", s.otHolidayHours())
                    .addValue("snapshot", json.writeValueAsString(Map.of("source", SOURCE, "days", s.days())));
            List<Map<String, Object>> existing = jdbc.queryForList(
                    "select id, unverified_days, confirmed_absent_days, review_status from payroll_records where period_id = :period and user_id = :u", r);
            String review = review(s.unverifiedDays(), s.pendingRequests(), existing.isEmpty() ? null : existing.getFirst());
            r.addValue("review", review);
            if (existing.isEmpty()) {
                jdbc.update("""
                        insert into payroll_records(period_id, user_id, period_month, employee_code, full_name, employee_type, store_id,
                            store_name, department_name, position_name, window_start, window_end, workdays_in_window, worked_days,
                            off_day_worked_days, worked_minutes, paid_leave_days, unpaid_leave_days, holiday_days, unverified_days,
                            late_count, early_count, late_minutes, early_minutes, missing_check_out_count, missing_check_in_count,
                            approved_excuse_count, pending_request_count, ot_weekday_hours, ot_weekend_hours, ot_holiday_hours,
                            review_status, attendance_snapshot, synced_at)
                        values (:period, :u, :month, :code, :name, :type, :storeId, :storeName, :dept, :pos, :ws, :we, :workdays, :worked,
                            :offWorked, :minutes, :paidLeave, :unpaidLeave, :holidays, :unverified, :late, :early, :lateMin, :earlyMin,
                            :missOut, :missIn, :excuses, :pending, :otWd, :otWe, :otHol, :review, cast(:snapshot as jsonb),
                            now() at time zone 'UTC')
                        """, r);
                added++;
            } else {
                // Số ngày HR đã xác nhận vắng không được vượt số ngày chưa xác minh mới.
                jdbc.update("""
                        update payroll_records set employee_code = :code, full_name = :name, store_name = :storeName,
                            department_name = :dept, position_name = :pos, window_start = :ws, window_end = :we,
                            workdays_in_window = :workdays, worked_days = :worked, off_day_worked_days = :offWorked,
                            worked_minutes = :minutes, paid_leave_days = :paidLeave, unpaid_leave_days = :unpaidLeave,
                            holiday_days = :holidays, unverified_days = :unverified,
                            confirmed_absent_days = least(confirmed_absent_days, :unverified),
                            late_count = :late, early_count = :early, late_minutes = :lateMin, early_minutes = :earlyMin,
                            missing_check_out_count = :missOut, missing_check_in_count = :missIn, approved_excuse_count = :excuses,
                            pending_request_count = :pending, ot_weekday_hours = :otWd, ot_weekend_hours = :otWe,
                            ot_holiday_hours = :otHol, review_status = :review, attendance_snapshot = cast(:snapshot as jsonb),
                            synced_at = now() at time zone 'UTC', calculated_at = null, updated_at = now() at time zone 'UTC'
                        where period_id = :period and user_id = :u
                        """, r);
                updated++;
            }
            kept.add(c.userId());
        }
        List<UUID> stale = jdbc.queryForList("select user_id from payroll_records where period_id = :period", p, UUID.class)
                .stream().filter(u -> !kept.contains(u)).toList();
        for (UUID u : stale) {
            jdbc.update("delete from payroll_records where period_id = :period and user_id = :u",
                    new MapSqlParameterSource("period", period.id()).addValue("u", u));
        }

        PayrollDtos.Settings policy = settings.settings();
        BigDecimal standard = "FIXED".equals(policy.standardDaysMode()) ? policy.fixedStandardDays()
                : BigDecimal.valueOf(attendance.workingDays(period.start(), period.end()));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("standardDaysMode", policy.standardDaysMode());
        snapshot.put("fixedStandardDays", policy.fixedStandardDays());
        snapshot.put("otWeekdayRate", policy.otWeekdayRate());
        snapshot.put("otWeekendRate", policy.otWeekendRate());
        snapshot.put("otHolidayRate", policy.otHolidayRate());
        snapshot.put("roundingUnit", policy.roundingUnit());
        snapshot.put("roundingMode", policy.roundingMode());
        snapshot.put("standardHoursPerDay", attendancePolicy.settings().standardHoursPerDay());
        jdbc.update("""
                update payroll_periods set status = 'DRAFT', standard_days = :standard, settings_snapshot = cast(:snapshot as jsonb),
                       attendance_synced_at = now() at time zone 'UTC', attendance_synced_by = :actor, calculated_at = null,
                       updated_at = now() at time zone 'UTC', version = version + 1
                where id = :period
                """, p.addValue("standard", standard).addValue("snapshot", json.writeValueAsString(snapshot)).addValue("actor", actorId));
        return new SyncResult(added, updated, stale.size(), skipped);
    }

    private static String review(BigDecimal unverified, int pending, Map<String, Object> existing) {
        if (pending > 0) return "NEEDS_REVIEW";
        if (unverified.signum() == 0) return "OK";
        if (existing != null && "CONFIRMED".equals(existing.get("review_status"))
                && unverified.compareTo((BigDecimal) existing.get("unverified_days")) == 0) return "CONFIRMED";
        return "NEEDS_REVIEW";
    }

    // ------------------------------------------------------------------ tính lương

    /** Tính lại một dòng lương (giữ khoản HR nhập tay; khoản từ hồ sơ / tăng ca / điều chỉnh kỳ trước được tạo lại). */
    void calculate(Period period, UUID recordId) {
        Map<String, Object> rec = jdbc.queryForMap("select * from payroll_records where id = :id", new MapSqlParameterSource("id", recordId));
        UUID userId = (UUID) rec.get("user_id");
        LocalDate ws = toDate(rec.get("window_start"));
        LocalDate we = toDate(rec.get("window_end"));
        List<SalaryStructureService.Version> versions = structures.versions(userId, ws, we);
        if (versions.isEmpty()) throw com.fashionsystem.fashion_system.exception.BusinessException.conflict(
                "Nhân viên " + rec.get("full_name") + " không còn hồ sơ lương hiệu lực trong kỳ, hãy đồng bộ lại");

        // Ngày được trả lương theo từng ngày (từ dữ liệu công đã đồng bộ), chia theo phiên bản lương.
        Map<LocalDate, BigDecimal> payableByDay = new LinkedHashMap<>();
        JsonNode snapshot = parse(String.valueOf(rec.get("attendance_snapshot")));
        for (JsonNode d : snapshot.path("days")) payableByDay.put(LocalDate.parse(d.path("date").asString()), d.path("payable").decimalValue());
        List<PayrollCalculator.Segment> segments = new ArrayList<>();
        for (SalaryStructureService.Version v : versions) {
            LocalDate from = v.from().isAfter(ws) ? v.from() : ws;
            LocalDate to = v.to() == null || v.to().isAfter(we) ? we : v.to();
            BigDecimal days = payableByDay.entrySet().stream().filter(e -> !e.getKey().isBefore(from) && !e.getKey().isAfter(to))
                    .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
            segments.add(new PayrollCalculator.Segment(from.toString(), to.toString(), v.baseSalary(), days));
        }
        // Ngày HR xác nhận vắng không lương trừ vào phiên bản cuối trước (lần lượt lùi về trước nếu không đủ).
        BigDecimal absent = (BigDecimal) rec.get("confirmed_absent_days");
        for (int i = segments.size() - 1; i >= 0 && absent.signum() > 0; i--) {
            PayrollCalculator.Segment s = segments.get(i);
            BigDecimal take = absent.min(s.payableDays());
            segments.set(i, new PayrollCalculator.Segment(s.from(), s.to(), s.baseSalary(), s.payableDays().subtract(take)));
            absent = absent.subtract(take);
        }

        SalaryStructureService.Version latest = versions.getLast();
        List<PayrollCalculator.Component> components = new ArrayList<>();
        Map<UUID, PayrollDtos.Component> catalog = new LinkedHashMap<>();
        settings.components(false).forEach(c -> catalog.put(c.id(), c));
        for (StructureItem item : structures.items(latest.id())) {
            PayrollDtos.Component c = catalog.get(item.componentId());
            components.add(new PayrollCalculator.Component(item.componentId(), item.code(), item.name(), item.kind(), item.calcMethod(),
                    item.amount() != null ? item.amount() : c.defaultAmount(), item.rate() != null ? item.rate() : c.defaultRate(),
                    c.sortOrder()));
        }

        // Điều chỉnh sau chốt của các kỳ trước (truy lĩnh / truy thu) đưa vào kỳ này.
        jdbc.update("""
                update payroll_adjustments a set applied_record_id = :rec
                where a.user_id = :u and a.phase = 'AFTER_CLOSE' and a.status = 'PENDING' and a.applied_record_id is null
                  and exists (select 1 from payroll_records o where o.id = a.record_id and o.period_month < :month)
                """, new MapSqlParameterSource("rec", recordId).addValue("u", userId).addValue("month", period.month()));
        jdbc.update("delete from payroll_items where record_id = :rec and source in ('STRUCTURE', 'SYSTEM', 'ADJUSTMENT')",
                new MapSqlParameterSource("rec", recordId));
        jdbc.update("""
                insert into payroll_items(record_id, code, name, kind, amount, source, adjustment_id, note, sort_order)
                select :rec, 'ADJUSTMENT', a.name, a.kind, a.amount, 'ADJUSTMENT', a.id,
                       'Điều chỉnh kỳ ' || to_char(o.period_month, 'MM/YYYY') || ': ' || a.reason, 400
                from payroll_adjustments a join payroll_records o on o.id = a.record_id
                where a.applied_record_id = :rec and a.status = 'PENDING'
                """, new MapSqlParameterSource("rec", recordId));
        List<PayrollCalculator.FixedLine> fixed = jdbc.query(
                "select kind, amount from payroll_items where record_id = :rec and source in ('MANUAL', 'ADJUSTMENT')",
                new MapSqlParameterSource("rec", recordId), (rs, i) -> new PayrollCalculator.FixedLine(rs.getString(1), rs.getBigDecimal(2)));

        JsonNode s = period.settingsSnapshot();
        PayrollCalculator.Settings calcSettings = new PayrollCalculator.Settings(s.path("otWeekdayRate").decimalValue(),
                s.path("otWeekendRate").decimalValue(), s.path("otHolidayRate").decimalValue(), s.path("roundingUnit").asInt(),
                s.path("roundingMode").asString(), s.path("standardHoursPerDay").decimalValue());
        int missing = ((Number) rec.get("missing_check_out_count")).intValue() + ((Number) rec.get("missing_check_in_count")).intValue();
        PayrollCalculator.Result result = PayrollCalculator.calculate(calcSettings, new PayrollCalculator.Input(period.standardDays(),
                segments, latest.baseSalary(), latest.overtimeEligible(), latest.overtimeHourlyRate(),
                ((BigDecimal) rec.get("worked_days")).add((BigDecimal) rec.get("off_day_worked_days")),
                ((Number) rec.get("late_count")).intValue(), ((Number) rec.get("early_count")).intValue(), missing,
                (BigDecimal) rec.get("unpaid_leave_days"), (BigDecimal) rec.get("confirmed_absent_days"),
                (BigDecimal) rec.get("ot_weekday_hours"), (BigDecimal) rec.get("ot_weekend_hours"), (BigDecimal) rec.get("ot_holiday_hours"),
                components, fixed));

        for (PayrollCalculator.Line line : result.lines()) {
            jdbc.update("""
                    insert into payroll_items(record_id, component_id, code, name, kind, calc_method, quantity, rate, amount, source, note, sort_order)
                    values (:rec, :comp, :code, :name, :kind, :method, :qty, :rate, :amount, :source, :note, :sort)
                    """, new MapSqlParameterSource("rec", recordId).addValue("comp", line.componentId(), java.sql.Types.OTHER)
                    .addValue("code", line.code()).addValue("name", line.name()).addValue("kind", line.kind())
                    .addValue("method", line.calcMethod()).addValue("qty", line.quantity()).addValue("rate", line.rate())
                    .addValue("amount", line.amount()).addValue("source", "OVERTIME".equals(line.kind()) ? "SYSTEM" : "STRUCTURE")
                    .addValue("note", line.note()).addValue("sort", line.sortOrder()));
        }
        String review = (String) rec.get("review_status");
        String reviewNote = (String) rec.get("review_note");
        if (result.netPay().signum() < 0) {
            review = "NEEDS_REVIEW";
            reviewNote = "Tổng khấu trừ lớn hơn thu nhập, thực nhận âm";
        }
        jdbc.update("""
                update payroll_records set salary_structure_id = :structure, base_salary = :base, standard_days = :standard,
                       pay_method = :method, bank_name = :bank, bank_account = :account, payable_days = :payable, base_pay = :basePay,
                       allowance_total = :allowance, bonus_total = :bonus, overtime_pay = :ot, deduction_total = :deduction,
                       gross_pay = :gross, net_pay = :net, review_status = :review, review_note = :reviewNote,
                       calculation_detail = cast(:detail as jsonb), calculated_at = now() at time zone 'UTC',
                       updated_at = now() at time zone 'UTC'
                where id = :rec
                """, new MapSqlParameterSource("rec", recordId).addValue("structure", latest.id()).addValue("base", latest.baseSalary())
                .addValue("standard", period.standardDays()).addValue("method", latest.payMethod()).addValue("bank", latest.bankName())
                .addValue("account", latest.bankAccount()).addValue("payable", result.payableDays()).addValue("basePay", result.basePay())
                .addValue("allowance", result.allowanceTotal()).addValue("bonus", result.bonusTotal()).addValue("ot", result.overtimePay())
                .addValue("deduction", result.deductionTotal()).addValue("gross", result.grossPay()).addValue("net", result.netPay())
                .addValue("review", review).addValue("reviewNote", reviewNote)
                .addValue("detail", json.writeValueAsString(result.detail())));
    }

    void log(UUID periodId, String action, String from, String to, String note, UUID actor) {
        jdbc.update("""
                insert into payroll_approvals(period_id, action, from_status, to_status, note, actor_id)
                values (:p, :a, :from, :to, :note, :actor)
                """, new MapSqlParameterSource("p", periodId).addValue("a", action).addValue("from", from).addValue("to", to)
                .addValue("note", note).addValue("actor", actor));
    }

    JsonNode parse(String text) {
        return text == null || "null".equals(text) ? null : json.readTree(text);
    }

    private static LocalDate toDate(Object value) {
        return value instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) value;
    }
}
