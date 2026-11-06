package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Overtime;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.OvertimeApprove;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.OvertimeCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Rejection;
import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.entity.OvertimeRequest;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.OvertimeRequestRepository;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đơn tăng ca: nhân viên đăng ký số giờ → quản lý / HR duyệt (có thể duyệt ít giờ hơn). Loại tăng ca (ngày thường,
 * ngày nghỉ tuần, ngày lễ) xác định theo lịch; chỉ giờ đã duyệt được đưa vào bảng lương.
 */
@Service
@Transactional(readOnly = true)
public class OvertimeRequestService {
    static final int MAX_DAYS = 30;

    private final OvertimeRequestRepository overtimes;
    private final AttendanceAccess access;
    private final AttendanceLockService locks;
    private final WorkCalendar calendar;
    private final RequestQueries queries;

    public OvertimeRequestService(OvertimeRequestRepository overtimes, AttendanceAccess access, AttendanceLockService locks,
                                  WorkCalendar calendar, RequestQueries queries) {
        this.overtimes = overtimes;
        this.access = access;
        this.locks = locks;
        this.calendar = calendar;
        this.queries = queries;
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Overtime create(UUID userId, OvertimeCreate request) {
        long offset = ChronoUnit.DAYS.between(LocalDate.now(), request.workDate());
        if (offset < -MAX_DAYS || offset > MAX_DAYS)
            throw BusinessException.badRequest("Chỉ đăng ký tăng ca trong vòng " + MAX_DAYS + " ngày trước / sau hôm nay");
        if (request.hours().remainder(new BigDecimal("0.25")).signum() != 0)
            throw BusinessException.badRequest("Số giờ tăng ca theo bước 15 phút (0.25 giờ)");
        locks.requireUnlocked(userId, request.workDate());
        Boolean open = queries.jdbc().queryForObject(
                "select exists (select 1 from overtime_requests where user_id = :u and work_date = :d and status in ('PENDING', 'APPROVED'))",
                new MapSqlParameterSource("u", userId).addValue("d", request.workDate()), Boolean.class);
        if (Boolean.TRUE.equals(open)) throw BusinessException.conflict("Bạn đã có đơn tăng ca cho ngày này");
        OvertimeRequest overtime = new OvertimeRequest();
        overtime.setCode(queries.nextCode("TC", "overtime_request_code_seq"));
        overtime.setUserId(userId);
        overtime.setStoreId(access.storeOf(userId));
        overtime.setWorkDate(request.workDate());
        overtime.setHours(request.hours());
        overtime.setOvertimeType(calendar.snapshot(request.workDate()).overtimeType(request.workDate()));
        overtime.setReason(request.reason().trim());
        overtime.setStatus("PENDING");
        overtime.setCreatedAt(LocalDateTime.now());
        try {
            overtimes.saveAndFlush(overtime);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("Bạn đã có đơn tăng ca cho ngày này");
        }
        return get(overtime.getId());
    }

    public List<Overtime> mine(UUID userId) {
        return queries.jdbc().query(SELECT + " where o.user_id = :u order by o.work_date desc limit 200",
                new MapSqlParameterSource("u", userId), OvertimeRequestService::map);
    }

    public Page<Overtime> list(Actor actor, RequestQueries.Filter filter, Pageable pageable) {
        return queries.page(actor, filter, pageable, SELECT, "overtime_requests o", "o", "o.work_date between :from and :to",
                OvertimeRequestService::map);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Overtime approve(Actor actor, UUID id, OvertimeApprove request) {
        OvertimeRequest overtime = pending(id);
        access.requireDecide(actor, actor.manage(), overtime.getUserId(), overtime.getStoreId());
        locks.requireUnlocked(overtime.getUserId(), overtime.getWorkDate());
        BigDecimal hours = request == null || request.approvedHours() == null ? overtime.getHours() : request.approvedHours();
        if (hours.compareTo(overtime.getHours()) > 0) throw BusinessException.badRequest("Số giờ duyệt không vượt số giờ đăng ký");
        overtime.setApprovedHours(hours);
        overtime.setStatus("APPROVED");
        overtime.setApprovedBy(actor.userId());
        overtime.setApprovedAt(LocalDateTime.now());
        overtime.setApproverNote(request == null ? null : RequestQueries.trim(request.note()));
        overtime.setUpdatedAt(LocalDateTime.now());
        return get(overtimes.saveAndFlush(overtime).getId());
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Overtime reject(Actor actor, UUID id, Rejection rejection) {
        OvertimeRequest overtime = pending(id);
        access.requireDecide(actor, actor.manage(), overtime.getUserId(), overtime.getStoreId());
        overtime.setStatus("REJECTED");
        overtime.setApprovedBy(actor.userId());
        overtime.setApprovedAt(LocalDateTime.now());
        overtime.setRejectionReason(rejection.reason().trim());
        overtime.setUpdatedAt(LocalDateTime.now());
        return get(overtimes.saveAndFlush(overtime).getId());
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Overtime cancel(UUID userId, UUID id) {
        OvertimeRequest overtime = pending(id);
        if (!overtime.getUserId().equals(userId)) throw BusinessException.forbidden("Bạn chỉ hủy được đơn của mình");
        overtime.setStatus("CANCELLED");
        overtime.setUpdatedAt(LocalDateTime.now());
        return get(overtimes.saveAndFlush(overtime).getId());
    }

    private OvertimeRequest pending(UUID id) {
        OvertimeRequest overtime = overtimes.findById(id).orElseThrow(() -> BusinessException.notFound("Đơn tăng ca không tồn tại"));
        if (!"PENDING".equals(overtime.getStatus())) throw BusinessException.conflict("Đơn đã được xử lý");
        return overtime;
    }

    private Overtime get(UUID id) {
        return queries.jdbc().query(SELECT + " where o.id = :id", new MapSqlParameterSource("id", id), OvertimeRequestService::map)
                .getFirst();
    }

    private static final String SELECT = """
            select o.*, u.employee_code, u.full_name, s.name as store_name, d.full_name as decided_by_name
            from overtime_requests o
            join users u on u.id = o.user_id
            left join stores s on s.id = o.store_id
            left join users d on d.id = o.approved_by
            """;

    private static Overtime map(ResultSet rs, int i) throws SQLException {
        return new Overtime(rs.getObject("id", UUID.class), rs.getString("code"), rs.getObject("user_id", UUID.class),
                rs.getString("employee_code"), rs.getString("full_name"), rs.getString("store_name"),
                rs.getObject("work_date", LocalDate.class), rs.getBigDecimal("hours"), rs.getBigDecimal("approved_hours"),
                rs.getString("overtime_type"), rs.getString("reason"), rs.getString("status"), rs.getString("decided_by_name"),
                DbTimes.fromDb(rs.getTimestamp("approved_at")), rs.getString("rejection_reason"), rs.getString("approver_note"),
                DbTimes.fromDb(rs.getTimestamp("created_at")));
    }
}
