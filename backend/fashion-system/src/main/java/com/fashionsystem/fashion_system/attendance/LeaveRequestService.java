package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Decision;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Leave;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveBalance;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveType;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.LeaveTypeRequest;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Rejection;
import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.entity.LeaveRequest;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.LeaveRequestRepository;
import com.fashionsystem.fashion_system.repository.LeaveTypeRepository;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghỉ phép: nhân viên tạo đơn → quản lý / HR duyệt hoặc từ chối. Số ngày nghỉ chỉ tính ngày làm việc (bỏ ngày nghỉ
 * tuần, ngày lễ), nửa ngày = 0.5. Loại nghỉ có hạn mức năm thì không vượt quá hạn mức (gồm cả đơn đang chờ).
 */
@Service
@Transactional(readOnly = true)
public class LeaveRequestService {
    static final int MAX_DAYS_BACK = 30;

    private final LeaveRequestRepository leaves;
    private final LeaveTypeRepository types;
    private final AttendanceAccess access;
    private final AttendanceLockService locks;
    private final WorkCalendar calendar;
    private final RequestQueries queries;

    public LeaveRequestService(LeaveRequestRepository leaves, LeaveTypeRepository types, AttendanceAccess access,
                               AttendanceLockService locks, WorkCalendar calendar, RequestQueries queries) {
        this.leaves = leaves;
        this.types = types;
        this.access = access;
        this.locks = locks;
        this.calendar = calendar;
        this.queries = queries;
    }

    // ------------------------------------------------------------------ loại nghỉ

    public List<LeaveType> types(boolean activeOnly) {
        return queries.jdbc().query("select * from leave_types" + (activeOnly ? " where active" : "") + " order by active desc, name",
                new MapSqlParameterSource(), (rs, i) -> new LeaveType(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getString("description"), rs.getBoolean("paid"), rs.getBigDecimal("annual_quota"),
                        rs.getBoolean("requires_documents"), rs.getBoolean("active")));
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public LeaveType saveType(Actor actor, UUID id, LeaveTypeRequest request) {
        if (!actor.globalAdmin()) throw BusinessException.forbidden("Chỉ quản trị chấm công cấp chuỗi được cấu hình loại nghỉ");
        String code = request.code().trim().toUpperCase();
        com.fashionsystem.fashion_system.entity.LeaveType type;
        if (id == null) {
            if (types.existsByCodeIgnoreCase(code)) throw BusinessException.conflict("Mã loại nghỉ đã tồn tại");
            type = new com.fashionsystem.fashion_system.entity.LeaveType();
            type.setCode(code);
            type.setRequiresApproval(true);
            type.setCreatedBy(actor.userId());
            type.setCreatedAt(LocalDateTime.now());
        } else {
            type = types.findById(id).orElseThrow(() -> BusinessException.notFound("Loại nghỉ không tồn tại"));
            if (!type.getCode().equalsIgnoreCase(code)) throw BusinessException.badRequest("Không đổi được mã loại nghỉ");
            type.setUpdatedAt(LocalDateTime.now());
        }
        type.setName(request.name().trim());
        type.setDescription(RequestQueries.trim(request.description()));
        type.setPaid(request.paid());
        type.setAnnualQuota(request.annualQuota());
        type.setRequiresDocuments(request.requiresDocuments());
        type.setActive(request.active());
        types.saveAndFlush(type);
        return types(false).stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ đơn nghỉ

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Leave create(UUID userId, LeaveCreate request) {
        com.fashionsystem.fashion_system.entity.LeaveType type = types.findById(request.leaveTypeId())
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .orElseThrow(() -> BusinessException.badRequest("Loại nghỉ không tồn tại hoặc đã ngừng sử dụng"));
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();
        if (end.isBefore(start)) throw BusinessException.badRequest("Ngày kết thúc phải từ ngày bắt đầu trở đi");
        if (ChronoUnit.DAYS.between(LocalDate.now(), start) < -MAX_DAYS_BACK)
            throw BusinessException.badRequest("Chỉ được tạo đơn cho ngày nghỉ trong vòng " + MAX_DAYS_BACK + " ngày trước");
        if (ChronoUnit.DAYS.between(start, end) > 366) throw BusinessException.badRequest("Một đơn nghỉ tối đa 1 năm");
        if (start.equals(end) && request.halfDayStart() && request.halfDayEnd())
            throw BusinessException.badRequest("Nghỉ một ngày: chọn nửa ngày hoặc cả ngày");
        BigDecimal days = calendar.snapshot(start, end).leaveDays(start, end, request.halfDayStart(), request.halfDayEnd());
        if (days.signum() == 0) throw BusinessException.badRequest("Khoảng ngày đã chọn không có ngày làm việc nào");
        locks.requireUnlocked(userId, start, end);
        requireNoOverlap(userId, start, end, null);
        requireQuota(userId, type, start, end, request.halfDayStart(), request.halfDayEnd(), true, null);

        LeaveRequest leave = new LeaveRequest();
        leave.setCode(queries.nextCode("NP", "leave_request_code_seq"));
        leave.setUserId(userId);
        leave.setStoreId(access.storeOf(userId));
        leave.setLeaveTypeId(type.getId());
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setIsHalfDayStart(request.halfDayStart());
        leave.setIsHalfDayEnd(request.halfDayEnd());
        leave.setTotalDays(days);
        leave.setReason(request.reason().trim());
        leave.setStatus("PENDING");
        leave.setCreatedAt(LocalDateTime.now());
        return get(leaves.saveAndFlush(leave).getId());
    }

    public List<Leave> mine(UUID userId) {
        return queries.jdbc().query(SELECT + " where l.user_id = :u order by l.start_date desc limit 200",
                new MapSqlParameterSource("u", userId), LeaveRequestService::map);
    }

    public Page<Leave> list(Actor actor, RequestQueries.Filter filter, Pageable pageable) {
        return queries.page(actor, filter, pageable, SELECT, "leave_requests l", "l", "l.start_date <= :to and l.end_date >= :from",
                LeaveRequestService::map);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Leave approve(Actor actor, UUID id, Decision decision) {
        LeaveRequest leave = pending(id);
        access.requireDecide(actor, actor.approveLeave(), leave.getUserId(), leave.getStoreId());
        locks.requireUnlocked(leave.getUserId(), leave.getStartDate(), leave.getEndDate());
        requireNoOverlap(leave.getUserId(), leave.getStartDate(), leave.getEndDate(), leave.getId());
        com.fashionsystem.fashion_system.entity.LeaveType type = types.findById(leave.getLeaveTypeId()).orElseThrow();
        requireQuota(leave.getUserId(), type, leave.getStartDate(), leave.getEndDate(), leave.getIsHalfDayStart(),
                leave.getIsHalfDayEnd(), false, leave.getId());
        leave.setStatus("APPROVED");
        leave.setApprovedBy(actor.userId());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setApproverNote(decision == null ? null : RequestQueries.trim(decision.note()));
        leave.setUpdatedAt(LocalDateTime.now());
        return get(leaves.saveAndFlush(leave).getId());
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Leave reject(Actor actor, UUID id, Rejection rejection) {
        LeaveRequest leave = pending(id);
        access.requireDecide(actor, actor.approveLeave(), leave.getUserId(), leave.getStoreId());
        leave.setStatus("REJECTED");
        leave.setApprovedBy(actor.userId());
        leave.setApprovedAt(LocalDateTime.now());
        leave.setRejectionReason(rejection.reason().trim());
        leave.setUpdatedAt(LocalDateTime.now());
        return get(leaves.saveAndFlush(leave).getId());
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Leave cancel(UUID userId, UUID id) {
        LeaveRequest leave = pending(id);
        if (!leave.getUserId().equals(userId)) throw BusinessException.forbidden("Bạn chỉ hủy được đơn của mình");
        leave.setStatus("CANCELLED");
        leave.setUpdatedAt(LocalDateTime.now());
        return get(leaves.saveAndFlush(leave).getId());
    }

    // ------------------------------------------------------------------ số dư

    public List<LeaveBalance> balance(UUID userId, int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        WorkCalendar.Snapshot cal = calendar.snapshot(from, to);
        List<LeaveRequest> rows = leavesOf(userId, from, to, null);
        return types(true).stream().map(t -> {
            BigDecimal used = sum(rows, t.id(), "APPROVED", from, to, cal);
            BigDecimal pending = sum(rows, t.id(), "PENDING", from, to, cal);
            BigDecimal remaining = t.annualQuota() == null ? null : t.annualQuota().subtract(used).subtract(pending);
            return new LeaveBalance(t.id(), t.code(), t.name(), t.paid(), t.annualQuota(), used, pending, remaining);
        }).toList();
    }

    private void requireNoOverlap(UUID userId, LocalDate start, LocalDate end, UUID except) {
        Boolean overlap = queries.jdbc().queryForObject("""
                select exists (select 1 from leave_requests where user_id = :u and status in ('PENDING', 'APPROVED')
                               and start_date <= :end and end_date >= :start and (cast(:except as uuid) is null or id <> :except))
                """, new MapSqlParameterSource("u", userId).addValue("start", start).addValue("end", end)
                .addValue("except", except, java.sql.Types.OTHER), Boolean.class);
        if (Boolean.TRUE.equals(overlap)) throw BusinessException.conflict("Đã có đơn nghỉ khác trùng khoảng ngày này");
    }

    /** Hạn mức tính theo từng năm dương lịch mà đơn đi qua. */
    private void requireQuota(UUID userId, com.fashionsystem.fashion_system.entity.LeaveType type, LocalDate start, LocalDate end,
                              boolean halfStart, boolean halfEnd, boolean countPending, UUID except) {
        if (type.getAnnualQuota() == null) return;
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);
            WorkCalendar.Snapshot cal = calendar.snapshot(from, to);
            LocalDate s = start.isBefore(from) ? from : start;
            LocalDate e = end.isAfter(to) ? to : end;
            BigDecimal requested = cal.leaveDays(s, e, halfStart && s.equals(start), halfEnd && e.equals(end));
            List<LeaveRequest> rows = leavesOf(userId, from, to, except);
            BigDecimal taken = sum(rows, type.getId(), "APPROVED", from, to, cal);
            if (countPending) taken = taken.add(sum(rows, type.getId(), "PENDING", from, to, cal));
            if (taken.add(requested).compareTo(type.getAnnualQuota()) > 0)
                throw BusinessException.badRequest("Vượt hạn mức " + type.getName() + " năm " + year + ": còn "
                        + type.getAnnualQuota().subtract(taken).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
                        + " ngày, đơn cần " + requested.stripTrailingZeros().toPlainString() + " ngày");
        }
    }

    private List<LeaveRequest> leavesOf(UUID userId, LocalDate from, LocalDate to, UUID except) {
        return queries.jdbc().query("""
                select id, leave_type_id, status, start_date, end_date, is_half_day_start, is_half_day_end from leave_requests
                where user_id = :u and status in ('PENDING', 'APPROVED') and start_date <= :to and end_date >= :from
                  and (cast(:except as uuid) is null or id <> :except)
                """, new MapSqlParameterSource("u", userId).addValue("from", from).addValue("to", to)
                .addValue("except", except, java.sql.Types.OTHER), (rs, i) -> {
            LeaveRequest l = new LeaveRequest();
            l.setId(rs.getObject("id", UUID.class));
            l.setLeaveTypeId(rs.getObject("leave_type_id", UUID.class));
            l.setStatus(rs.getString("status"));
            l.setStartDate(rs.getObject("start_date", LocalDate.class));
            l.setEndDate(rs.getObject("end_date", LocalDate.class));
            l.setIsHalfDayStart(rs.getBoolean("is_half_day_start"));
            l.setIsHalfDayEnd(rs.getBoolean("is_half_day_end"));
            return l;
        });
    }

    /** Tổng ngày nghỉ của các đơn, cắt theo khoảng [from, to]. */
    private static BigDecimal sum(List<LeaveRequest> rows, UUID typeId, String status, LocalDate from, LocalDate to,
                                  WorkCalendar.Snapshot cal) {
        return rows.stream().filter(l -> l.getLeaveTypeId().equals(typeId) && status.equals(l.getStatus())).map(l -> {
            LocalDate s = l.getStartDate().isBefore(from) ? from : l.getStartDate();
            LocalDate e = l.getEndDate().isAfter(to) ? to : l.getEndDate();
            return cal.leaveDays(s, e, l.getIsHalfDayStart() && s.equals(l.getStartDate()), l.getIsHalfDayEnd() && e.equals(l.getEndDate()));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LeaveRequest pending(UUID id) {
        LeaveRequest leave = leaves.findById(id).orElseThrow(() -> BusinessException.notFound("Đơn nghỉ không tồn tại"));
        if (!"PENDING".equals(leave.getStatus())) throw BusinessException.conflict("Đơn đã được xử lý");
        return leave;
    }

    private Leave get(UUID id) {
        return queries.jdbc().query(SELECT + " where l.id = :id", new MapSqlParameterSource("id", id), LeaveRequestService::map).getFirst();
    }

    private static final String SELECT = """
            select l.*, t.name as type_name, t.paid, u.employee_code, u.full_name, s.name as store_name, d.full_name as decided_by_name
            from leave_requests l
            join leave_types t on t.id = l.leave_type_id
            join users u on u.id = l.user_id
            left join stores s on s.id = l.store_id
            left join users d on d.id = l.approved_by
            """;

    private static Leave map(ResultSet rs, int i) throws SQLException {
        return new Leave(rs.getObject("id", UUID.class), rs.getString("code"), rs.getObject("user_id", UUID.class),
                rs.getString("employee_code"), rs.getString("full_name"), rs.getString("store_name"),
                rs.getObject("leave_type_id", UUID.class), rs.getString("type_name"), rs.getBoolean("paid"),
                rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                rs.getBoolean("is_half_day_start"), rs.getBoolean("is_half_day_end"), rs.getBigDecimal("total_days"),
                rs.getString("reason"), rs.getString("status"), rs.getString("decided_by_name"),
                DbTimes.fromDb(rs.getTimestamp("approved_at")), rs.getString("rejection_reason"), rs.getString("approver_note"),
                DbTimes.fromDb(rs.getTimestamp("created_at")));
    }
}
