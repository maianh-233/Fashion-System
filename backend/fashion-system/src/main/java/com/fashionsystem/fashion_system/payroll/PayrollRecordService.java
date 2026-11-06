package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payroll.PayrollAccess.Actor;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.AfterCloseAdjustment;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Adjustment;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.ConfirmDays;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Item;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.ItemRequest;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.Payment;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordDetail;
import com.fashionsystem.fashion_system.payroll.PayrollDtos.RecordRow;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Chi tiết và điều chỉnh từng dòng lương. Khi kỳ chưa gửi duyệt: thêm / bỏ khoản nhập tay, xác nhận ngày công chưa xác
 * minh — đều ghi lịch sử điều chỉnh và tính lại ngay. Khi kỳ đã chốt: chỉ tạo điều chỉnh riêng (truy lĩnh / truy thu)
 * được đưa vào kỳ lương kế tiếp của nhân viên.
 */
@Service
@Transactional(readOnly = true)
public class PayrollRecordService {
    private final NamedParameterJdbcTemplate jdbc;
    private final PayrollEngine engine;
    private final ObjectMapper json;

    public PayrollRecordService(NamedParameterJdbcTemplate jdbc, PayrollEngine engine, ObjectMapper json) {
        this.jdbc = jdbc;
        this.engine = engine;
        this.json = json;
    }

    private record Ref(UUID recordId, UUID periodId, UUID userId, UUID storeId, String status, boolean confidential,
                       BigDecimal unverified, int pending, boolean calculated) {
    }

    private Ref ref(UUID recordId) {
        List<Ref> rows = jdbc.query("""
                select r.id, r.period_id, r.user_id, p.store_id, p.status, r.unverified_days, r.pending_request_count,
                       p.calculated_at is not null as calculated,
                       coalesce((select s.confidential from salary_structures s where s.id = r.salary_structure_id), false) as confidential
                from payroll_records r join payroll_periods p on p.id = r.period_id where r.id = :id
                """, new MapSqlParameterSource("id", recordId), (rs, i) -> new Ref(rs.getObject("id", UUID.class),
                rs.getObject("period_id", UUID.class), rs.getObject("user_id", UUID.class), rs.getObject("store_id", UUID.class),
                rs.getString("status"), rs.getBoolean("confidential"), rs.getBigDecimal("unverified_days"),
                rs.getInt("pending_request_count"), rs.getBoolean("calculated")));
        if (rows.isEmpty()) throw BusinessException.notFound("Phiếu lương không tồn tại");
        return rows.getFirst();
    }

    private Ref visible(Actor actor, UUID recordId) {
        PayrollAccess.require(actor.permissions().view(), "Bạn không có quyền xem bảng lương");
        Ref ref = ref(recordId);
        PayrollAccess.requirePeriodScope(actor, ref.storeId());
        if (ref.confidential() && !actor.admin()) throw BusinessException.forbidden("Phiếu lương này được bảo mật");
        return ref;
    }

    private Ref editable(Actor actor, UUID recordId) {
        Ref ref = visible(actor, recordId);
        PayrollAccess.require(actor.permissions().manage(), "Bạn không có quyền điều chỉnh bảng lương");
        engine.period(ref.periodId(), true);
        ref = ref(recordId);
        if (!PayrollPeriodService.EDITABLE.contains(ref.status()))
            throw BusinessException.conflict("Bảng lương đang chờ duyệt hoặc đã chốt; hãy dùng điều chỉnh sau chốt");
        if (actor.userId().equals(ref.userId())) throw BusinessException.forbidden("Không thể tự điều chỉnh lương của chính mình");
        return ref;
    }

    public RecordDetail detail(Actor actor, UUID recordId) {
        Ref ref = visible(actor, recordId);
        boolean editable = actor.permissions().manage() && PayrollPeriodService.EDITABLE.contains(ref.status())
                && !actor.userId().equals(ref.userId());
        return load(recordId, editable);
    }

    RecordDetail load(UUID recordId, boolean editable) {
        Map<String, Object> r = jdbc.queryForMap("""
                select r.*, p.code as period_code, p.status as period_status from payroll_records r
                join payroll_periods p on p.id = r.period_id where r.id = :id
                """, new MapSqlParameterSource("id", recordId));
        RecordRow row = jdbc.query("select r.* from payroll_records r where r.id = :id", new MapSqlParameterSource("id", recordId),
                PayrollPeriodService::mapRow).getFirst();
        List<Item> items = jdbc.query("select * from payroll_items where record_id = :id order by sort_order, created_at",
                new MapSqlParameterSource("id", recordId), (rs, i) -> new Item(rs.getObject("id", UUID.class),
                        rs.getObject("component_id", UUID.class), rs.getString("code"), rs.getString("name"), rs.getString("kind"),
                        rs.getString("calc_method"), rs.getBigDecimal("quantity"), rs.getBigDecimal("rate"), rs.getBigDecimal("amount"),
                        rs.getString("source"), rs.getString("note")));
        List<Adjustment> adjustments = jdbc.query("""
                select a.*, u.full_name, ap.code as applied_code, a.old_value::text as old_text, a.new_value::text as new_text
                from payroll_adjustments a left join users u on u.id = a.created_by
                left join payroll_records ar on ar.id = a.applied_record_id
                left join payroll_periods ap on ap.id = ar.period_id
                where a.record_id = :id or a.applied_record_id = :id order by a.created_at desc
                """, new MapSqlParameterSource("id", recordId), (rs, i) -> new Adjustment(rs.getObject("id", UUID.class),
                rs.getString("phase"), rs.getString("action"), rs.getString("kind"), rs.getString("name"), rs.getBigDecimal("amount"),
                rs.getString("reason"), rs.getString("status"), rs.getString("applied_code"), rs.getString("full_name"),
                DbTimes.fromDb(rs.getTimestamp("created_at")), engine.parse(rs.getString("old_text")), engine.parse(rs.getString("new_text"))));
        List<Payment> payments = jdbc.query("""
                select p.*, u.full_name from payroll_payments p left join users u on u.id = p.confirmed_by where p.record_id = :id
                """, new MapSqlParameterSource("id", recordId), (rs, i) -> new Payment(rs.getBigDecimal("amount"), rs.getString("method"),
                rs.getString("reference"), DbTimes.fromDb(rs.getTimestamp("paid_at")), rs.getString("full_name"), rs.getString("note")));
        String status = (String) r.get("period_status");
        var snapshot = engine.parse(String.valueOf(r.get("attendance_snapshot")));
        return new RecordDetail(row, (String) r.get("period_code"), toDate(r.get("period_month")), status,
                toDate(r.get("window_start")), toDate(r.get("window_end")), (BigDecimal) r.get("workdays_in_window"),
                (BigDecimal) r.get("off_day_worked_days"), num(r, "worked_minutes"), num(r, "late_count"), num(r, "early_count"),
                num(r, "late_minutes"), num(r, "early_minutes"), num(r, "missing_check_out_count"), num(r, "missing_check_in_count"),
                num(r, "approved_excuse_count"), num(r, "pending_request_count"), (BigDecimal) r.get("ot_weekday_hours"),
                (BigDecimal) r.get("ot_weekend_hours"), (BigDecimal) r.get("ot_holiday_hours"), (String) r.get("pay_method"),
                (String) r.get("bank_name"), (String) r.get("bank_account"), DbTimes.fromDb(r.get("synced_at")),
                DbTimes.fromDb(r.get("calculated_at")), items, adjustments, payments.isEmpty() ? null : payments.getFirst(),
                snapshot == null ? null : snapshot.path("days"), engine.parse(String.valueOf(r.get("calculation_detail"))), editable,
                PayrollPeriodService.CLOSED.contains(status));
    }

    // ------------------------------------------------------------------ trước khi chốt

    @Transactional
    public RecordDetail addItem(Actor actor, UUID recordId, ItemRequest request) {
        Ref ref = editable(actor, recordId);
        UUID componentId = request.componentId();
        if (componentId != null) {
            List<String> kinds = jdbc.queryForList("select kind from payroll_components where id = :id and active",
                    new MapSqlParameterSource("id", componentId), String.class);
            if (kinds.isEmpty() || !kinds.getFirst().equals(request.kind()))
                throw BusinessException.badRequest("Khoản lương không hợp lệ hoặc không khớp loại");
        }
        UUID itemId = jdbc.queryForObject("""
                insert into payroll_items(record_id, component_id, name, kind, amount, source, note, sort_order, created_by)
                values (:rec, :comp, :name, :kind, :amount, 'MANUAL', :note, 350, :actor) returning id
                """, new MapSqlParameterSource("rec", recordId).addValue("comp", componentId, java.sql.Types.OTHER)
                .addValue("name", request.name().trim()).addValue("kind", request.kind()).addValue("amount", request.amount())
                .addValue("note", trim(request.note())).addValue("actor", actor.userId()), UUID.class);
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("itemId", itemId.toString());
        value.put("name", request.name().trim());
        value.put("kind", request.kind());
        value.put("amount", request.amount());
        adjustment(ref, "IN_PERIOD", "ADD_ITEM", request.kind(), request.name().trim(), request.amount(), null, value,
                request.reason().trim(), "APPLIED", actor);
        recalc(ref);
        return detail(actor, recordId);
    }

    @Transactional
    public RecordDetail removeItem(Actor actor, UUID recordId, UUID itemId, String reason) {
        Ref ref = editable(actor, recordId);
        List<Map<String, Object>> rows = jdbc.queryForList("select name, kind, amount, source from payroll_items where id = :id and record_id = :rec",
                new MapSqlParameterSource("id", itemId).addValue("rec", recordId));
        if (rows.isEmpty()) throw BusinessException.notFound("Khoản lương không tồn tại");
        Map<String, Object> item = rows.getFirst();
        if (!"MANUAL".equals(item.get("source")))
            throw BusinessException.badRequest("Chỉ bỏ được khoản nhập tay; khoản từ hồ sơ lương hãy sửa hồ sơ rồi tính lại");
        jdbc.update("delete from payroll_items where id = :id", new MapSqlParameterSource("id", itemId));
        adjustment(ref, "IN_PERIOD", "REMOVE_ITEM", (String) item.get("kind"), (String) item.get("name"), (BigDecimal) item.get("amount"),
                item, null, reason.trim(), "APPLIED", actor);
        recalc(ref);
        return detail(actor, recordId);
    }

    /** HR xác minh ngày công thiếu dữ liệu: bao nhiêu ngày là vắng không lương (còn lại vẫn được trả lương). */
    @Transactional
    public RecordDetail confirmDays(Actor actor, UUID recordId, ConfirmDays request) {
        Ref ref = editable(actor, recordId);
        if (ref.pending() > 0)
            throw BusinessException.conflict("Nhân viên còn đơn chờ duyệt trong kỳ. Hãy xử lý ở mục Chấm công rồi đồng bộ lại.");
        if (request.confirmedAbsentDays().compareTo(ref.unverified()) > 0)
            throw BusinessException.badRequest("Số ngày vắng không vượt quá " + ref.unverified().stripTrailingZeros().toPlainString()
                    + " ngày chưa xác minh");
        if (request.confirmedAbsentDays().remainder(new BigDecimal("0.5")).signum() != 0)
            throw BusinessException.badRequest("Số ngày vắng theo bước 0.5 ngày");
        Map<String, Object> before = jdbc.queryForMap("select confirmed_absent_days, review_status, review_note from payroll_records where id = :id",
                new MapSqlParameterSource("id", recordId));
        jdbc.update("""
                update payroll_records set confirmed_absent_days = :days, review_status = 'CONFIRMED', review_note = :note,
                       updated_at = now() at time zone 'UTC' where id = :id
                """, new MapSqlParameterSource("id", recordId).addValue("days", request.confirmedAbsentDays())
                .addValue("note", request.note().trim()));
        adjustment(ref, "IN_PERIOD", "CONFIRM_DAYS", null, "Xác nhận ngày công", null, before,
                Map.of("confirmedAbsentDays", request.confirmedAbsentDays(), "unverifiedDays", ref.unverified()),
                request.note().trim(), "APPLIED", actor);
        recalc(ref);
        return detail(actor, recordId);
    }

    // ------------------------------------------------------------------ sau khi chốt

    @Transactional
    public RecordDetail afterClose(Actor actor, UUID recordId, AfterCloseAdjustment request) {
        Ref ref = visible(actor, recordId);
        PayrollAccess.require(actor.permissions().manage(), "Bạn không có quyền điều chỉnh bảng lương");
        if (!PayrollPeriodService.CLOSED.contains(ref.status()))
            throw BusinessException.conflict("Kỳ lương chưa chốt: hãy điều chỉnh trực tiếp trên bảng lương");
        if (actor.userId().equals(ref.userId())) throw BusinessException.forbidden("Không thể tự điều chỉnh lương của chính mình");
        adjustment(ref, "AFTER_CLOSE", "CORRECTION", request.kind(), request.name().trim(), request.amount(), null,
                Map.of("kind", request.kind(), "name", request.name().trim(), "amount", request.amount()), request.reason().trim(),
                "PENDING", actor);
        return detail(actor, recordId);
    }

    /** Hủy điều chỉnh sau chốt khi chưa được đưa vào kỳ lương đã gửi duyệt. */
    @Transactional
    public RecordDetail cancelAdjustment(Actor actor, UUID recordId, UUID adjustmentId) {
        Ref ref = visible(actor, recordId);
        PayrollAccess.require(actor.permissions().manage(), "Bạn không có quyền điều chỉnh bảng lương");
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select a.status, a.applied_record_id, p.status as applied_status, p.id as applied_period
                from payroll_adjustments a left join payroll_records ar on ar.id = a.applied_record_id
                left join payroll_periods p on p.id = ar.period_id
                where a.id = :id and a.record_id = :rec and a.phase = 'AFTER_CLOSE'
                """, new MapSqlParameterSource("id", adjustmentId).addValue("rec", recordId));
        if (rows.isEmpty()) throw BusinessException.notFound("Điều chỉnh không tồn tại");
        Map<String, Object> a = rows.getFirst();
        if (!"PENDING".equals(a.get("status"))) throw BusinessException.conflict("Điều chỉnh đã được áp dụng hoặc đã hủy");
        if (a.get("applied_status") != null && !PayrollPeriodService.EDITABLE.contains((String) a.get("applied_status")))
            throw BusinessException.conflict("Điều chỉnh đã nằm trong kỳ lương đang chờ duyệt, không hủy được");
        jdbc.update("update payroll_adjustments set status = 'CANCELLED' where id = :id", new MapSqlParameterSource("id", adjustmentId));
        if (a.get("applied_record_id") != null) {
            jdbc.update("delete from payroll_items where adjustment_id = :id", new MapSqlParameterSource("id", adjustmentId));
            PayrollEngine.Period target = engine.period((UUID) a.get("applied_period"), true);
            if (target.calculatedAt() != null) engine.calculate(target, (UUID) a.get("applied_record_id"));
        }
        return detail(actor, ref.recordId());
    }

    // ------------------------------------------------------------------ dùng chung

    private void recalc(Ref ref) {
        if (!ref.calculated()) return;
        PayrollEngine.Period period = engine.period(ref.periodId(), false);
        engine.calculate(period, ref.recordId());
    }

    private void adjustment(Ref ref, String phase, String action, String kind, String name, BigDecimal amount, Object oldValue,
                            Object newValue, String reason, String status, Actor actor) {
        jdbc.update("""
                insert into payroll_adjustments(record_id, user_id, phase, action, kind, name, amount, old_value, new_value, reason, status, created_by)
                values (:rec, :u, :phase, :action, :kind, :name, :amount, cast(:old as jsonb), cast(:new as jsonb), :reason, :status, :actor)
                """, new MapSqlParameterSource("rec", ref.recordId()).addValue("u", ref.userId()).addValue("phase", phase)
                .addValue("action", action).addValue("kind", kind).addValue("name", name).addValue("amount", amount)
                .addValue("old", oldValue == null ? null : json.writeValueAsString(oldValue))
                .addValue("new", newValue == null ? null : json.writeValueAsString(newValue))
                .addValue("reason", reason).addValue("status", status).addValue("actor", actor.userId()));
    }

    private static int num(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).intValue();
    }

    private static LocalDate toDate(Object value) {
        return value instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) value;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
