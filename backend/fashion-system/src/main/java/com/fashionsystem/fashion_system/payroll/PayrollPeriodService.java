package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payroll.PayrollAccess.Actor;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Actions;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Approval;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PaymentRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PaymentResult;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodCreate;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.PeriodSummary;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordRow;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.SyncResult;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Kỳ lương: HR tạo kỳ → đồng bộ công → tính lương (tính thử, tính lại được) → gửi duyệt (khóa dữ liệu công) →
 * phê duyệt / từ chối → chốt (APPROVED, phiếu lương phát hành) → xác nhận thanh toán (PAID). Bảng lương đã chốt không sửa
 * trực tiếp; sai lệch xử lý bằng điều chỉnh sau chốt.
 */
@Service
@Transactional(readOnly = true)
public class PayrollPeriodService {
    static final Set<String> EDITABLE = Set.of("DRAFT", "CALCULATED");
    static final Set<String> CLOSED = Set.of("APPROVED", "PAID");

    private final NamedParameterJdbcTemplate jdbc;
    private final PayrollEngine engine;
    private final PayrollNotifier notifier;

    public PayrollPeriodService(NamedParameterJdbcTemplate jdbc, PayrollEngine engine, PayrollNotifier notifier) {
        this.jdbc = jdbc;
        this.engine = engine;
        this.notifier = notifier;
    }

    // ------------------------------------------------------------------ danh sách / chi tiết

    public Page<PeriodSummary> list(Actor actor, String month, UUID storeId, String status, Pageable pageable) {
        PayrollAccess.require(actor.permissions().view(), "Bạn không có quyền xem bảng lương");
        MapSqlParameterSource params = PayrollAccess.params(actor)
                .addValue("month", StringUtils.hasText(month) ? YearMonth.parse(month).atDay(1) : null, java.sql.Types.DATE)
                .addValue("store", PayrollAccess.storeFilter(actor, storeId), java.sql.Types.OTHER)
                .addValue("status", StringUtils.hasText(status) ? status : null, java.sql.Types.VARCHAR);
        String where = " where " + PayrollAccess.periodGuard("p") + """
                 and (cast(:month as date) is null or p.period_month = :month)
                 and (cast(:store as uuid) is null or p.store_id = :store)
                 and (cast(:status as varchar) is null or p.status = :status)
                """;
        Integer total = jdbc.queryForObject("select count(*) from payroll_periods p" + where, params, Integer.class);
        List<PeriodSummary> rows = jdbc.query(SUMMARY + where + " order by p.period_month desc, s.name nulls first limit :limit offset :offset",
                params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), PayrollPeriodService::mapSummary);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    public PeriodDetail detail(Actor actor, UUID id) {
        PayrollAccess.require(actor.permissions().view(), "Bạn không có quyền xem bảng lương");
        PeriodSummary period = summary(id);
        PayrollAccess.requirePeriodScope(actor, period.storeId());
        List<Approval> approvals = jdbc.query("""
                select a.*, u.full_name from payroll_approvals a left join users u on u.id = a.actor_id
                where a.period_id = :id order by a.created_at
                """, new MapSqlParameterSource("id", id), (rs, i) -> new Approval(rs.getString("action"), rs.getString("from_status"),
                rs.getString("to_status"), rs.getString("note"), rs.getString("full_name"), DbTimes.fromDb(rs.getTimestamp("created_at"))));
        List<String> blockers = blockers(id, period);
        String st = period.status();
        var p = actor.permissions();
        Actions actions = new Actions(p.manage() && EDITABLE.contains(st), p.manage() && EDITABLE.contains(st) && period.attendanceSyncedAt() != null,
                p.manage() && "CALCULATED".equals(st) && blockers.isEmpty(), p.approve() && "PENDING_APPROVAL".equals(st),
                p.pay() && "APPROVED".equals(st), p.manage() && Set.of("DRAFT", "CALCULATED", "PENDING_APPROVAL").contains(st),
                p.manage() && EDITABLE.contains(st));
        return new PeriodDetail(period, approvals, actions, blockers);
    }

    /** Lý do chưa gửi duyệt được. */
    private List<String> blockers(UUID id, PeriodSummary period) {
        List<String> list = new ArrayList<>();
        if (period.attendanceSyncedAt() == null) list.add("Chưa đồng bộ dữ liệu công");
        if (period.employeeCount() == 0) list.add("Kỳ lương chưa có nhân viên nào");
        Integer notCalculated = jdbc.queryForObject("select count(*) from payroll_records where period_id = :id and calculated_at is null",
                new MapSqlParameterSource("id", id), Integer.class);
        if (notCalculated != null && notCalculated > 0) list.add(notCalculated + " nhân viên chưa được tính lương sau lần đồng bộ gần nhất");
        if (period.needsReviewCount() > 0)
            list.add(period.needsReviewCount() + " nhân viên cần xem xét (ngày công chưa xác minh, đơn chờ duyệt hoặc thực nhận âm)");
        return list;
    }

    public Page<RecordRow> records(Actor actor, UUID periodId, String keyword, String reviewStatus, Pageable pageable) {
        PeriodSummary period = summary(periodId);
        PayrollAccess.require(actor.permissions().view(), "Bạn không có quyền xem bảng lương");
        PayrollAccess.requirePeriodScope(actor, period.storeId());
        MapSqlParameterSource params = PayrollAccess.params(actor).addValue("period", periodId)
                .addValue("kw", StringUtils.hasText(keyword) ? "%" + keyword.trim().toLowerCase() + "%" : null, java.sql.Types.VARCHAR)
                .addValue("review", StringUtils.hasText(reviewStatus) ? reviewStatus : null, java.sql.Types.VARCHAR);
        String where = " where r.period_id = :period and " + PayrollAccess.confidentialGuard("r") + """
                 and (cast(:kw as varchar) is null or lower(r.full_name) like :kw or lower(coalesce(r.employee_code, '')) like :kw
                      or lower(coalesce(r.department_name, '')) like :kw)
                 and (cast(:review as varchar) is null or r.review_status = :review)
                """;
        Integer total = jdbc.queryForObject("select count(*) from payroll_records r" + where, params, Integer.class);
        List<RecordRow> rows = jdbc.query("select r.* from payroll_records r" + where + " order by r.full_name limit :limit offset :offset",
                params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), PayrollPeriodService::mapRow);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    // ------------------------------------------------------------------ quy trình

    @Transactional
    public PeriodDetail create(Actor actor, PeriodCreate request) {
        PayrollAccess.require(actor.permissions().manage(), "Bạn không có quyền lập bảng lương");
        YearMonth month = YearMonth.parse(request.month());
        if (month.isAfter(YearMonth.now().plusMonths(1))) throw BusinessException.badRequest("Chỉ tạo kỳ lương đến tháng sau");
        UUID storeId = request.storeId();
        if (!actor.global()) {
            if (storeId == null) throw BusinessException.forbidden("HR cửa hàng chỉ tạo kỳ lương cho cửa hàng của mình");
            PayrollAccess.requirePeriodScope(actor, storeId);
        }
        String storeCode = "VP";
        if (storeId != null) {
            List<String> codes = jdbc.queryForList("select code from stores where id = :id and active", new MapSqlParameterSource("id", storeId), String.class);
            if (codes.isEmpty()) throw BusinessException.badRequest("Cửa hàng không tồn tại hoặc đã ngừng hoạt động");
            storeCode = codes.getFirst();
        }
        Boolean exists = jdbc.queryForObject("""
                select exists (select 1 from payroll_periods where period_month = :month and status <> 'CANCELLED'
                               and store_id is not distinct from cast(:store as uuid))
                """, new MapSqlParameterSource("month", month.atDay(1)).addValue("store", storeId, java.sql.Types.OTHER), Boolean.class);
        if (Boolean.TRUE.equals(exists))
            throw BusinessException.conflict("Đã có kỳ lương tháng " + month + " cho " + (storeId == null ? "Văn phòng" : "cửa hàng này"));
        String code = "BL-" + month.toString().replace("-", "") + "-" + storeCode;
        Long seq = jdbc.queryForObject("select nextval('payroll_period_code_seq')", new MapSqlParameterSource(), Long.class);
        UUID id;
        try {
            id = jdbc.queryForObject("""
                    insert into payroll_periods(code, period_month, start_date, end_date, store_id, note, created_by)
                    values (:code, :month, :start, :end, :store, :note, :actor) returning id
                    """, new MapSqlParameterSource("code", code + "-" + seq).addValue("month", month.atDay(1)).addValue("start", month.atDay(1))
                    .addValue("end", month.atEndOfMonth()).addValue("store", storeId, java.sql.Types.OTHER)
                    .addValue("note", StringUtils.hasText(request.note()) ? request.note().trim() : null).addValue("actor", actor.userId()), UUID.class);
        } catch (DuplicateKeyException e) {
            throw BusinessException.conflict("Đã có kỳ lương tháng " + month + " cho " + (storeId == null ? "Văn phòng" : "cửa hàng này"));
        }
        engine.log(id, "CREATE", null, "DRAFT", null, actor.userId());
        return detail(actor, id);
    }

    @Transactional
    public SyncResult sync(Actor actor, UUID id) {
        PayrollEngine.Period period = editable(actor, id);
        SyncResult result = engine.sync(period, actor.userId());
        engine.log(id, "SYNC_ATTENDANCE", period.status(), "DRAFT", "Thêm " + result.added() + ", cập nhật " + result.updated()
                + ", loại " + result.removed() + ", bỏ qua " + result.skipped().size() + " (nguồn " + PayrollEngine.SOURCE + ")", actor.userId());
        return result;
    }

    @Transactional
    public PeriodDetail calculate(Actor actor, UUID id) {
        PayrollEngine.Period period = editable(actor, id);
        if (period.syncedAt() == null) throw BusinessException.conflict("Hãy đồng bộ dữ liệu công trước khi tính lương");
        List<UUID> records = jdbc.queryForList("select id from payroll_records where period_id = :id", new MapSqlParameterSource("id", id), UUID.class);
        if (records.isEmpty()) throw BusinessException.conflict("Kỳ lương chưa có nhân viên nào");
        for (UUID record : records) engine.calculate(period, record);
        jdbc.update("""
                update payroll_periods set status = 'CALCULATED', calculated_at = now() at time zone 'UTC', calculated_by = :actor,
                       updated_at = now() at time zone 'UTC', version = version + 1 where id = :id
                """, new MapSqlParameterSource("id", id).addValue("actor", actor.userId()));
        engine.log(id, "CALCULATE", period.status(), "CALCULATED", records.size() + " nhân viên", actor.userId());
        return detail(actor, id);
    }

    @Transactional
    public PeriodDetail submit(Actor actor, UUID id, String note) {
        PayrollEngine.Period period = locked(actor, id);
        PayrollAccess.require(actor.permissions().manage(), "Bạn không có quyền gửi duyệt bảng lương");
        requireStatus(period, "CALCULATED");
        List<String> blockers = blockers(id, summary(id));
        if (!blockers.isEmpty()) throw BusinessException.conflict("Chưa gửi duyệt được: " + String.join("; ", blockers));
        try {
            jdbc.update("""
                    insert into attendance_locks(user_id, period_month, payroll_period_id)
                    select user_id, period_month, period_id from payroll_records where period_id = :id
                    """, new MapSqlParameterSource("id", id));
        } catch (DuplicateKeyException e) {
            throw BusinessException.conflict("Có nhân viên đã bị khóa công tháng này bởi kỳ lương khác");
        }
        transition(id, "PENDING_APPROVAL", "submitted_at = now() at time zone 'UTC', submitted_by = :actor", actor);
        engine.log(id, "SUBMIT", "CALCULATED", "PENDING_APPROVAL", trim(note), actor.userId());
        notifier.approvalRequested(id, period.code(), actor.userId());
        return detail(actor, id);
    }

    @Transactional
    public PeriodDetail approve(Actor actor, UUID id, String note) {
        PayrollEngine.Period period = locked(actor, id);
        PayrollAccess.require(actor.permissions().approve(), "Bạn không có quyền phê duyệt bảng lương");
        requireStatus(period, "PENDING_APPROVAL");
        transition(id, "APPROVED", "approved_at = now() at time zone 'UTC', approved_by = :actor", actor);
        jdbc.update("""
                update payroll_adjustments set status = 'APPLIED'
                where status = 'PENDING' and applied_record_id in (select id from payroll_records where period_id = :id)
                """, new MapSqlParameterSource("id", id));
        engine.log(id, "APPROVE", "PENDING_APPROVAL", "APPROVED", trim(note), actor.userId());
        notifier.payslipsPublished(id, period.month(), actor.userId());
        return detail(actor, id);
    }

    @Transactional
    public PeriodDetail reject(Actor actor, UUID id, String reason) {
        PayrollEngine.Period period = locked(actor, id);
        PayrollAccess.require(actor.permissions().approve(), "Bạn không có quyền phê duyệt bảng lương");
        requireStatus(period, "PENDING_APPROVAL");
        jdbc.update("delete from attendance_locks where payroll_period_id = :id", new MapSqlParameterSource("id", id));
        jdbc.update("""
                update payroll_periods set status = 'CALCULATED', last_rejection_reason = :reason, submitted_at = null, submitted_by = null,
                       updated_at = now() at time zone 'UTC', version = version + 1 where id = :id
                """, new MapSqlParameterSource("id", id).addValue("reason", reason.trim()));
        engine.log(id, "REJECT", "PENDING_APPROVAL", "CALCULATED", reason.trim(), actor.userId());
        return detail(actor, id);
    }

    @Transactional
    public PeriodDetail cancel(Actor actor, UUID id, String reason) {
        PayrollEngine.Period period = locked(actor, id);
        PayrollAccess.require(actor.permissions().manage(), "Bạn không có quyền hủy kỳ lương");
        if (!Set.of("DRAFT", "CALCULATED", "PENDING_APPROVAL").contains(period.status()))
            throw BusinessException.conflict("Kỳ lương đã chốt không hủy được; sai lệch hãy tạo điều chỉnh sau chốt");
        MapSqlParameterSource p = new MapSqlParameterSource("id", id).addValue("reason", reason.trim()).addValue("actor", actor.userId());
        jdbc.update("delete from attendance_locks where payroll_period_id = :id", p);
        jdbc.update("""
                update payroll_adjustments set applied_record_id = null
                where status = 'PENDING' and applied_record_id in (select id from payroll_records where period_id = :id)
                """, p);
        jdbc.update("update payroll_records set active = false, updated_at = now() at time zone 'UTC' where period_id = :id", p);
        jdbc.update("""
                update payroll_periods set status = 'CANCELLED', cancelled_at = now() at time zone 'UTC', cancelled_by = :actor,
                       cancel_reason = :reason, updated_at = now() at time zone 'UTC', version = version + 1 where id = :id
                """, p);
        engine.log(id, "CANCEL", period.status(), "CANCELLED", reason.trim(), actor.userId());
        return detail(actor, id);
    }

    @Transactional
    public PaymentResult pay(Actor actor, UUID id, PaymentRequest request) {
        PayrollEngine.Period period = locked(actor, id);
        PayrollAccess.require(actor.permissions().pay(), "Bạn không có quyền xác nhận thanh toán lương");
        if (!"APPROVED".equals(period.status()))
            throw BusinessException.conflict("PAID".equals(period.status()) ? "Kỳ lương đã thanh toán đủ" : "Chỉ thanh toán bảng lương đã chốt");
        List<UUID> targets = request == null || request.recordIds() == null || request.recordIds().isEmpty()
                ? jdbc.queryForList("select id from payroll_records where period_id = :id", new MapSqlParameterSource("id", id), UUID.class)
                : request.recordIds();
        LocalDateTime paidAt = request != null && request.paidDate() != null ? request.paidDate().atTime(LocalDateTime.now().toLocalTime())
                : LocalDateTime.now();
        if (paidAt.toLocalDate().isAfter(LocalDate.now())) throw BusinessException.badRequest("Ngày thanh toán không được ở tương lai");
        int paid = 0, already = 0;
        for (UUID recordId : targets) {
            int inserted = jdbc.update("""
                    insert into payroll_payments(record_id, amount, method, reference, paid_at, confirmed_by, note)
                    select r.id, r.net_pay, coalesce(r.pay_method, 'CASH'), :ref, :paidAt, :actor, :note
                    from payroll_records r where r.id = :rec and r.period_id = :id
                    on conflict (record_id) do nothing
                    """, new MapSqlParameterSource("rec", recordId).addValue("id", id).addValue("actor", actor.userId())
                    .addValue("ref", request == null ? null : trim(request.reference()))
                    .addValue("note", request == null ? null : trim(request.note())).addValue("paidAt", DbTimes.toDb(paidAt)));
            if (inserted == 0) {
                Boolean exists = jdbc.queryForObject("select exists (select 1 from payroll_records where id = :rec and period_id = :id)",
                        new MapSqlParameterSource("rec", recordId).addValue("id", id), Boolean.class);
                if (!Boolean.TRUE.equals(exists)) throw BusinessException.badRequest("Phiếu lương không thuộc kỳ lương này");
                already++;
                continue;
            }
            jdbc.update("update payroll_records set payment_status = 'PAID', updated_at = now() at time zone 'UTC' where id = :rec",
                    new MapSqlParameterSource("rec", recordId));
            paid++;
        }
        if (paid == 0 && already > 0) throw BusinessException.conflict("Các phiếu lương đã chọn đều đã được thanh toán");
        Integer unpaid = jdbc.queryForObject("select count(*) from payroll_records where period_id = :id and payment_status = 'UNPAID'",
                new MapSqlParameterSource("id", id), Integer.class);
        String status = "APPROVED";
        if (unpaid != null && unpaid == 0) {
            transition(id, "PAID", "paid_at = now() at time zone 'UTC'", actor);
            status = "PAID";
        }
        engine.log(id, "PAY", "APPROVED", status, "Xác nhận thanh toán " + paid + " phiếu lương"
                + (request != null && StringUtils.hasText(request.reference()) ? " (" + request.reference().trim() + ")" : ""), actor.userId());
        return new PaymentResult(paid, already, status);
    }

    // ------------------------------------------------------------------ dùng chung

    private PayrollEngine.Period locked(Actor actor, UUID id) {
        PayrollEngine.Period period = engine.period(id, true);
        PayrollAccess.requirePeriodScope(actor, period.storeId());
        return period;
    }

    PayrollEngine.Period editable(Actor actor, UUID id) {
        PayrollEngine.Period period = locked(actor, id);
        PayrollAccess.require(actor.permissions().manage(), "Bạn không có quyền lập bảng lương");
        if (!EDITABLE.contains(period.status()))
            throw BusinessException.conflict("Bảng lương đang chờ duyệt hoặc đã chốt, không thay đổi được");
        return period;
    }

    private static void requireStatus(PayrollEngine.Period period, String status) {
        if (!status.equals(period.status())) throw BusinessException.conflict("Trạng thái kỳ lương không phù hợp (" + period.status() + ")");
    }

    private void transition(UUID id, String status, String extra, Actor actor) {
        jdbc.update("update payroll_periods set status = :status, " + extra
                        + ", updated_at = now() at time zone 'UTC', version = version + 1 where id = :id",
                new MapSqlParameterSource("id", id).addValue("status", status).addValue("actor", actor.userId()));
    }

    PeriodSummary summary(UUID id) {
        List<PeriodSummary> rows = jdbc.query(SUMMARY + " where p.id = :id", new MapSqlParameterSource("id", id), PayrollPeriodService::mapSummary);
        if (rows.isEmpty()) throw BusinessException.notFound("Kỳ lương không tồn tại");
        return rows.getFirst();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static final String SUMMARY = """
            select p.*, s.name as store_name, sb.full_name as submitted_by_name, ab.full_name as approved_by_name,
                   cb.full_name as created_by_name, agg.*
            from payroll_periods p
            left join stores s on s.id = p.store_id
            left join users sb on sb.id = p.submitted_by
            left join users ab on ab.id = p.approved_by
            left join users cb on cb.id = p.created_by
            left join lateral (select count(*) as employee_count,
                                      count(*) filter (where r.review_status = 'NEEDS_REVIEW') as needs_review,
                                      count(*) filter (where r.payment_status = 'PAID') as paid_count,
                                      coalesce(sum(r.gross_pay), 0) as total_gross, coalesce(sum(r.deduction_total), 0) as total_deduction,
                                      coalesce(sum(r.net_pay), 0) as total_net
                               from payroll_records r where r.period_id = p.id) agg on true
            """;

    static PeriodSummary mapSummary(ResultSet rs, int i) throws SQLException {
        return new PeriodSummary(rs.getObject("id", UUID.class), rs.getString("code"), rs.getObject("period_month", LocalDate.class),
                rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class), rs.getObject("store_id", UUID.class),
                rs.getString("store_name"), rs.getString("status"), rs.getBigDecimal("standard_days"), rs.getInt("employee_count"),
                rs.getInt("needs_review"), rs.getInt("paid_count"), rs.getBigDecimal("total_gross"), rs.getBigDecimal("total_deduction"),
                rs.getBigDecimal("total_net"), DbTimes.fromDb(rs.getTimestamp("attendance_synced_at")),
                DbTimes.fromDb(rs.getTimestamp("calculated_at")), DbTimes.fromDb(rs.getTimestamp("submitted_at")),
                rs.getString("submitted_by_name"), DbTimes.fromDb(rs.getTimestamp("approved_at")), rs.getString("approved_by_name"),
                DbTimes.fromDb(rs.getTimestamp("paid_at")), rs.getString("cancel_reason"), rs.getString("last_rejection_reason"),
                rs.getString("note"), rs.getString("created_by_name"), DbTimes.fromDb(rs.getTimestamp("created_at")));
    }

    static RecordRow mapRow(ResultSet rs, int i) throws SQLException {
        return new RecordRow(rs.getObject("id", UUID.class), rs.getObject("period_id", UUID.class),
                rs.getObject("period_month", LocalDate.class), rs.getObject("user_id", UUID.class),
                rs.getString("employee_code"), rs.getString("full_name"), rs.getString("employee_type"), rs.getString("store_name"),
                rs.getString("department_name"), rs.getString("position_name"), rs.getBigDecimal("base_salary"),
                rs.getBigDecimal("standard_days"), rs.getBigDecimal("payable_days"), rs.getBigDecimal("worked_days"),
                rs.getBigDecimal("paid_leave_days"), rs.getBigDecimal("unpaid_leave_days"), rs.getBigDecimal("holiday_days"),
                rs.getBigDecimal("unverified_days"), rs.getBigDecimal("confirmed_absent_days"), rs.getBigDecimal("base_pay"),
                rs.getBigDecimal("allowance_total"), rs.getBigDecimal("bonus_total"), rs.getBigDecimal("overtime_pay"),
                rs.getBigDecimal("deduction_total"), rs.getBigDecimal("gross_pay"), rs.getBigDecimal("net_pay"),
                rs.getString("review_status"), rs.getString("review_note"), rs.getString("payment_status"),
                rs.getTimestamp("calculated_at") != null);
    }
}
