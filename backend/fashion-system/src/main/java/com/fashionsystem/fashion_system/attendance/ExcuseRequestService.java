package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Decision;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Excuse;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.ExcuseCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Rejection;
import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import com.fashionsystem.fashion_system.entity.ExcuseRequest;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.AttendanceRecordRepository;
import com.fashionsystem.fashion_system.repository.ExcuseRequestRepository;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đơn giải trình quên check-in / check-out: nhân viên không tự sửa công, chỉ gửi đơn. Đơn chờ duyệt không được tính
 * vào công; khi duyệt, bản ghi chấm công được cập nhật và lưu lịch sử điều chỉnh.
 */
@Service
@Transactional(readOnly = true)
public class ExcuseRequestService {
    static final Set<String> TYPES = Set.of("FORGOT_CHECK_IN", "FORGOT_CHECK_OUT");
    /** Hạn nộp giải trình tính từ ngày chấm công. */
    static final int MAX_DAYS_BACK = 30;

    private final ExcuseRequestRepository excuses;
    private final AttendanceRecordRepository records;
    private final AttendanceRecordService recordService;
    private final AttendanceAccess access;
    private final AttendanceLockService locks;
    private final RequestQueries queries;

    public ExcuseRequestService(ExcuseRequestRepository excuses, AttendanceRecordRepository records,
                                AttendanceRecordService recordService, AttendanceAccess access, AttendanceLockService locks,
                                RequestQueries queries) {
        this.excuses = excuses;
        this.records = records;
        this.recordService = recordService;
        this.access = access;
        this.locks = locks;
        this.queries = queries;
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Excuse create(UUID userId, ExcuseCreate request) {
        String type = request.type().trim().toUpperCase();
        if (!TYPES.contains(type)) throw BusinessException.badRequest("Loại giải trình không hợp lệ");
        LocalDate today = LocalDate.now();
        if (request.date().isAfter(today)) throw BusinessException.badRequest("Không thể giải trình cho ngày trong tương lai");
        if (ChronoUnit.DAYS.between(request.date(), today) > MAX_DAYS_BACK)
            throw BusinessException.badRequest("Chỉ được giải trình trong vòng " + MAX_DAYS_BACK + " ngày");
        locks.requireUnlocked(userId, request.date());

        AttendanceRecord record = records.findByUserIdAndAttendanceDate(userId, request.date()).orElse(null);
        LocalDateTime checkIn = null;
        LocalDateTime checkOut = null;
        if (type.equals("FORGOT_CHECK_IN")) {
            if (request.proposedCheckIn() == null) throw BusinessException.badRequest("Cần nhập giờ vào đề xuất");
            if (record != null && record.getCheckInTime() != null) throw BusinessException.conflict("Ngày này đã có giờ check-in");
            checkIn = request.proposedCheckIn();
            // Quên cả hai lần chấm: cho phép kèm giờ ra trong cùng đơn.
            if (request.proposedCheckOut() != null && (record == null || record.getCheckOutTime() == null))
                checkOut = request.proposedCheckOut();
        } else {
            if (request.proposedCheckOut() == null) throw BusinessException.badRequest("Cần nhập giờ ra đề xuất");
            if (record != null && record.getCheckOutTime() != null) throw BusinessException.conflict("Ngày này đã có giờ check-out");
            if (record == null || record.getCheckInTime() == null)
                throw BusinessException.badRequest("Ngày này chưa có giờ check-in, hãy chọn loại \"Quên check-in\"");
            checkOut = request.proposedCheckOut();
        }
        AttendanceRecordService.validateTimes(request.date(),
                checkIn != null ? checkIn : record == null ? null : record.getCheckInTime(),
                checkOut != null ? checkOut : record == null ? null : record.getCheckOutTime());

        Boolean open = queries.jdbc().queryForObject("""
                select exists (select 1 from excuse_requests where user_id = :u and attendance_date = :d and excuse_type = :t
                               and status in ('PENDING', 'APPROVED'))
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("u", userId).addValue("d", request.date())
                .addValue("t", type), Boolean.class);
        if (Boolean.TRUE.equals(open)) throw BusinessException.conflict("Bạn đã có đơn giải trình cùng loại cho ngày này");

        ExcuseRequest excuse = new ExcuseRequest();
        excuse.setCode(queries.nextCode("GT", "attendance_excuse_code_seq"));
        excuse.setUserId(userId);
        excuse.setStoreId(access.storeOf(userId));
        excuse.setAttendanceDate(request.date());
        excuse.setExcuseType(type);
        excuse.setProposedCheckIn(checkIn);
        excuse.setProposedCheckOut(checkOut);
        excuse.setReason(request.reason().trim());
        excuse.setStatus("PENDING");
        excuse.setCreatedAt(LocalDateTime.now());
        try {
            excuses.saveAndFlush(excuse);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("Bạn đã có đơn giải trình cùng loại cho ngày này");
        }
        return get(excuse.getId());
    }

    public List<Excuse> mine(UUID userId) {
        return jdbc().query(SELECT + " where e.user_id = :u order by e.created_at desc limit 200",
                new MapSqlParameterSource("u", userId), ExcuseRequestService::map);
    }

    public Page<Excuse> list(Actor actor, RequestQueries.Filter filter, Pageable pageable) {
        return queries.page(actor, filter, pageable, SELECT, "excuse_requests e", "e", "e.attendance_date between :from and :to",
                ExcuseRequestService::map);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Excuse approve(Actor actor, UUID id, Decision decision) {
        ExcuseRequest excuse = pending(id);
        access.requireDecide(actor, actor.manage(), excuse.getUserId(), excuse.getStoreId());
        locks.requireUnlocked(excuse.getUserId(), excuse.getAttendanceDate());
        recordService.applyExcuse(excuse.getUserId(), excuse.getAttendanceDate(), excuse.getProposedCheckIn(),
                excuse.getProposedCheckOut(), excuse.getStoreId(), "Duyệt giải trình " + excuse.getCode() + ": " + excuse.getReason(),
                actor.userId());
        excuse.setStatus("APPROVED");
        excuse.setApprovedBy(actor.userId());
        excuse.setApprovedAt(LocalDateTime.now());
        excuse.setApproverNote(decision == null ? null : RequestQueries.trim(decision.note()));
        excuse.setUpdatedAt(LocalDateTime.now());
        return get(excuses.saveAndFlush(excuse).getId());
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Excuse reject(Actor actor, UUID id, Rejection rejection) {
        ExcuseRequest excuse = pending(id);
        access.requireDecide(actor, actor.manage(), excuse.getUserId(), excuse.getStoreId());
        excuse.setStatus("REJECTED");
        excuse.setApprovedBy(actor.userId());
        excuse.setApprovedAt(LocalDateTime.now());
        excuse.setRejectionReason(rejection.reason().trim());
        excuse.setUpdatedAt(LocalDateTime.now());
        return get(excuses.saveAndFlush(excuse).getId());
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Excuse cancel(UUID userId, UUID id) {
        ExcuseRequest excuse = pending(id);
        if (!excuse.getUserId().equals(userId)) throw BusinessException.forbidden("Bạn chỉ hủy được đơn của mình");
        excuse.setStatus("CANCELLED");
        excuse.setUpdatedAt(LocalDateTime.now());
        return get(excuses.saveAndFlush(excuse).getId());
    }

    private ExcuseRequest pending(UUID id) {
        ExcuseRequest excuse = excuses.findById(id).orElseThrow(() -> BusinessException.notFound("Đơn giải trình không tồn tại"));
        if (!"PENDING".equals(excuse.getStatus())) throw BusinessException.conflict("Đơn đã được xử lý");
        return excuse;
    }

    private Excuse get(UUID id) {
        return jdbc().query(SELECT + " where e.id = :id", new MapSqlParameterSource("id", id), ExcuseRequestService::map).getFirst();
    }

    private NamedParameterJdbcTemplate jdbc() {
        return queries.jdbc();
    }

    private static final String SELECT = """
            select e.*, u.employee_code, u.full_name, s.name as store_name, d.full_name as decided_by_name
            from excuse_requests e
            join users u on u.id = e.user_id
            left join stores s on s.id = e.store_id
            left join users d on d.id = e.approved_by
            """;

    private static Excuse map(ResultSet rs, int i) throws SQLException {
        return new Excuse(rs.getObject("id", UUID.class), rs.getString("code"), rs.getObject("user_id", UUID.class),
                rs.getString("employee_code"), rs.getString("full_name"), rs.getString("store_name"),
                rs.getObject("attendance_date", LocalDate.class), rs.getString("excuse_type"),
                DbTimes.fromDb(rs.getTimestamp("proposed_check_in")), DbTimes.fromDb(rs.getTimestamp("proposed_check_out")),
                rs.getString("reason"), rs.getString("status"), rs.getString("decided_by_name"),
                DbTimes.fromDb(rs.getTimestamp("approved_at")), rs.getString("rejection_reason"), rs.getString("approver_note"),
                DbTimes.fromDb(rs.getTimestamp("created_at")));
    }
}
