package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.AdjustRequest;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.AuditEntry;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.CheckRequest;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Record;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.RecordCreate;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.RecordDetail;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Today;
import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.entity.AttendanceAuditLog;
import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.AttendanceAuditLogRepository;
import com.fashionsystem.fashion_system.repository.AttendanceRecordRepository;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Chấm công vào / ra (giờ máy chủ), xác minh vị trí cửa hàng, tính đi trễ / về sớm; bảng công cho HR theo phạm vi;
 * điều chỉnh công có lý do và lưu lịch sử (dữ liệu cũ, dữ liệu mới, người sửa).
 */
@Service
@Transactional(readOnly = true)
public class AttendanceRecordService {
    static final String COMPLETED = "COMPLETED";
    static final String MISSING_CHECK_OUT = "MISSING_CHECK_OUT";
    static final String MISSING_CHECK_IN = "MISSING_CHECK_IN";

    private final AttendanceRecordRepository records;
    private final AttendanceAuditLogRepository auditLogs;
    private final NamedParameterJdbcTemplate jdbc;
    private final AttendanceAccess access;
    private final AttendancePolicy policy;
    private final WorkCalendar calendar;
    private final AttendanceLockService locks;
    private final ObjectMapper json;

    public AttendanceRecordService(AttendanceRecordRepository records, AttendanceAuditLogRepository auditLogs,
                                   NamedParameterJdbcTemplate jdbc, AttendanceAccess access, AttendancePolicy policy,
                                   WorkCalendar calendar, AttendanceLockService locks, ObjectMapper json) {
        this.records = records;
        this.auditLogs = auditLogs;
        this.jdbc = jdbc;
        this.access = access;
        this.policy = policy;
        this.calendar = calendar;
        this.locks = locks;
        this.json = json;
    }

    // ------------------------------------------------------------------ nhân viên tự chấm công

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Record checkIn(UUID userId, CheckRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        locks.requireUnlocked(userId, today);
        AttendanceRecord record = records.findByUserIdAndAttendanceDate(userId, today).orElse(null);
        if (record != null && record.getCheckInTime() != null) throw BusinessException.conflict("Bạn đã check-in hôm nay rồi");
        if (record != null) throw BusinessException.conflict(
                "Bạn đã check-out hôm nay nhưng chưa check-in. Hãy gửi đơn giải trình quên check-in.");

        Location location = verifyLocation(userId, request);
        AttendancePolicy.Rule rule = policy.ruleFor(userId, location.storeId(), today);
        record = new AttendanceRecord();
        record.setUserId(userId);
        record.setAttendanceDate(today);
        record.setStoreId(location.storeId());
        record.setShiftId(rule.shiftId());
        record.setCheckInTime(now);
        record.setCheckInLocationLat(request == null ? null : request.latitude());
        record.setCheckInLocationLon(request == null ? null : request.longitude());
        record.setCheckInDistanceM(location.distance());
        record.setLateMinutes(rule.lateMinutes(now.toLocalTime()));
        record.setStatus(MISSING_CHECK_OUT);
        record.setSource("SELF");
        record.setCreatedAt(now);
        record = saveUnique(record);
        writeExpected(record.getId(), rule.start(), rule.end());
        log(record.getId(), "CHECK_IN", null, snapshot(record), "Tự check-in", userId);
        return record(record.getId());
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Record checkOut(UUID userId, CheckRequest request) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        locks.requireUnlocked(userId, today);
        AttendanceRecord record = records.findByUserIdAndAttendanceDate(userId, today).orElse(null);
        if (record != null && record.getCheckOutTime() != null) throw BusinessException.conflict("Bạn đã check-out hôm nay rồi");

        Location location = verifyLocation(userId, request);
        JsonNode before = record == null ? null : snapshot(record);
        if (record == null) {
            // Quên check-in: vẫn ghi nhận giờ ra, nhân viên gửi giải trình để bổ sung giờ vào.
            AttendancePolicy.Rule rule = policy.ruleFor(userId, location.storeId(), today);
            record = new AttendanceRecord();
            record.setUserId(userId);
            record.setAttendanceDate(today);
            record.setStoreId(location.storeId());
            record.setShiftId(rule.shiftId());
            record.setSource("SELF");
            record.setCreatedAt(now);
        }
        Expected expected = expected(record);
        record.setCheckOutTime(now);
        record.setCheckOutLocationLat(request == null ? null : request.latitude());
        record.setCheckOutLocationLon(request == null ? null : request.longitude());
        record.setCheckOutDistanceM(location.distance());
        recompute(record, expected);
        record.setUpdatedAt(now);
        record = saveUnique(record);
        writeExpected(record.getId(), expected.start(), expected.end());
        log(record.getId(), "CHECK_OUT", before, snapshot(record), "Tự check-out", userId);
        return record(record.getId());
    }

    public Today today(UUID userId) {
        LocalDate today = LocalDate.now();
        UUID storeId = access.storeOf(userId);
        AttendancePolicy.Rule rule = policy.ruleFor(userId, storeId, today);
        WorkCalendar.Snapshot cal = calendar.snapshot(today);
        List<String> leave = jdbc.queryForList("""
                select t.name from leave_requests l join leave_types t on t.id = l.leave_type_id
                where l.user_id = :u and l.status = 'APPROVED' and :d between l.start_date and l.end_date limit 1
                """, new MapSqlParameterSource("u", userId).addValue("d", today), String.class);
        String storeName = storeId == null ? null : jdbc.queryForObject("select name from stores where id = :id",
                new MapSqlParameterSource("id", storeId), String.class);
        Record record = records.findByUserIdAndAttendanceDate(userId, today).map(r -> record(r.getId())).orElse(null);
        return new Today(today, storeId == null ? "GLOBAL" : "STORE", storeName, rule.start(), rule.end(),
                cal.holidays().get(today), cal.weeklyOff(today), leave.isEmpty() ? null : leave.getFirst(), record,
                storeId != null && policy.settings().requireStoreLocation());
    }

    public List<Record> history(UUID userId, LocalDate from, LocalDate to) {
        requireRange(from, to);
        return jdbc.query(SELECT + " where r.user_id = :u and r.attendance_date between :from and :to order by r.attendance_date desc",
                new MapSqlParameterSource("u", userId).addValue("from", from).addValue("to", to).addValue("today", LocalDate.now()),
                AttendanceRecordService::map);
    }

    // ------------------------------------------------------------------ HR: bảng công

    public record Filter(LocalDate from, LocalDate to, UUID storeId, String employeeType, String status, String keyword,
                         UUID userId) {
    }

    public Page<Record> list(Actor actor, Filter filter, Pageable pageable) {
        requireRange(filter.from(), filter.to());
        MapSqlParameterSource params = filterParams(actor, filter);
        String where = where(filter);
        Integer total = jdbc.queryForObject("select count(*) from attendance_records r join users u on u.id = r.user_id " + where,
                params, Integer.class);
        List<Record> rows = jdbc.query(SELECT + where + " order by r.attendance_date desc, u.full_name limit :limit offset :offset",
                params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), AttendanceRecordService::map);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    /** Toàn bộ dòng khớp bộ lọc (xuất Excel). */
    List<Record> all(Actor actor, Filter filter) {
        requireRange(filter.from(), filter.to());
        return jdbc.query(SELECT + where(filter) + " order by r.attendance_date, u.full_name", filterParams(actor, filter),
                AttendanceRecordService::map);
    }

    private MapSqlParameterSource filterParams(Actor actor, Filter filter) {
        String type = StringUtils.hasText(filter.employeeType()) ? filter.employeeType().toUpperCase() : null;
        return AttendanceAccess.params(actor, access.storeFilter(actor, filter.storeId()), type)
                .addValue("from", filter.from()).addValue("to", filter.to()).addValue("today", LocalDate.now())
                .addValue("status", StringUtils.hasText(filter.status()) ? filter.status().toUpperCase() : null, java.sql.Types.VARCHAR)
                .addValue("kw", StringUtils.hasText(filter.keyword()) ? "%" + filter.keyword().trim().toLowerCase() + "%" : null,
                        java.sql.Types.VARCHAR)
                .addValue("userId", filter.userId(), java.sql.Types.OTHER);
    }

    private static String where(Filter filter) {
        return " where r.attendance_date between :from and :to and " + AttendanceAccess.guard("r.user_id", "r.store_id") + """
                 and (cast(:userId as uuid) is null or r.user_id = :userId)
                 and (cast(:kw as varchar) is null or lower(u.full_name) like :kw or lower(coalesce(u.employee_code, '')) like :kw)
                 and (cast(:status as varchar) is null
                      or (:status in ('COMPLETED', 'MISSING_CHECK_OUT', 'MISSING_CHECK_IN') and r.status = :status
                          and not (:status = 'MISSING_CHECK_OUT' and r.attendance_date = :today))
                      or (:status = 'WORKING' and r.status = 'MISSING_CHECK_OUT' and r.attendance_date = :today)
                      or (:status = 'LATE' and r.late_minutes > 0)
                      or (:status = 'EARLY' and r.early_minutes > 0))
                """;
    }

    public RecordDetail detail(Actor actor, UUID id) {
        Record record = record(id);
        access.requireSee(actor, record.userId(), record.storeId());
        List<AuditEntry> history = jdbc.query("""
                select l.action, l.reason, a.full_name, l.created_at, l.old_data::text as old_data, l.new_data::text as new_data
                from attendance_audit_logs l left join users a on a.id = l.actor_id
                where l.attendance_record_id = :id order by l.created_at desc
                """, new MapSqlParameterSource("id", id), (rs, i) -> new AuditEntry(rs.getString("action"), rs.getString("reason"),
                rs.getString("full_name"), DbTimes.fromDb(rs.getTimestamp("created_at")), rs.getString("old_data"),
                rs.getString("new_data")));
        return new RecordDetail(record, history);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Record adjust(Actor actor, UUID id, AdjustRequest request) {
        AttendanceRecord record = records.findById(id).orElseThrow(() -> BusinessException.notFound("Bản ghi chấm công không tồn tại"));
        access.requireDecide(actor, actor.manage(), record.getUserId(), record.getStoreId());
        locks.requireUnlocked(record.getUserId(), record.getAttendanceDate());
        if (request.checkIn() == null && request.checkOut() == null)
            throw BusinessException.badRequest("Cần nhập giờ vào hoặc giờ ra");
        JsonNode before = snapshot(record);
        if (request.checkIn() != null) record.setCheckInTime(request.checkIn().truncatedTo(ChronoUnit.SECONDS));
        if (request.checkOut() != null) record.setCheckOutTime(request.checkOut().truncatedTo(ChronoUnit.SECONDS));
        validateTimes(record.getAttendanceDate(), record.getCheckInTime(), record.getCheckOutTime());
        recompute(record, expected(record));
        record.setSource("ADJUSTED");
        record.setUpdatedAt(LocalDateTime.now());
        records.saveAndFlush(record);
        log(id, "ADJUST", before, snapshot(record), request.reason().trim(), actor.userId());
        return record(id);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Record create(Actor actor, RecordCreate request) {
        UUID storeId = access.storeOf(request.userId());
        access.requireDecide(actor, actor.manage(), request.userId(), storeId);
        locks.requireUnlocked(request.userId(), request.date());
        if (request.date().isAfter(LocalDate.now())) throw BusinessException.badRequest("Không thể tạo công cho ngày trong tương lai");
        if (records.findByUserIdAndAttendanceDate(request.userId(), request.date()).isPresent())
            throw BusinessException.conflict("Nhân viên đã có bản ghi chấm công ngày này, hãy điều chỉnh bản ghi đó");
        validateTimes(request.date(), request.checkIn(), request.checkOut());
        AttendancePolicy.Rule rule = policy.ruleFor(request.userId(), storeId, request.date());
        AttendanceRecord record = new AttendanceRecord();
        record.setUserId(request.userId());
        record.setAttendanceDate(request.date());
        record.setStoreId(storeId);
        record.setShiftId(rule.shiftId());
        record.setCheckInTime(request.checkIn().truncatedTo(ChronoUnit.SECONDS));
        record.setCheckOutTime(request.checkOut().truncatedTo(ChronoUnit.SECONDS));
        recompute(record, new Expected(rule.start(), rule.end()));
        record.setSource("ADJUSTED");
        record.setCreatedAt(LocalDateTime.now());
        record = saveUnique(record);
        writeExpected(record.getId(), rule.start(), rule.end());
        log(record.getId(), "CREATE", null, snapshot(record), request.reason().trim(), actor.userId());
        return record(record.getId());
    }

    // ------------------------------------------------------------------ dùng chung

    /** Ghi giờ vào / ra từ đơn giải trình đã duyệt. */
    AttendanceRecord applyExcuse(UUID userId, LocalDate date, LocalDateTime checkIn, LocalDateTime checkOut, UUID storeId,
                                 String reason, UUID actorId) {
        AttendanceRecord record = records.findByUserIdAndAttendanceDate(userId, date).orElse(null);
        JsonNode before = record == null ? null : snapshot(record);
        if (record == null) {
            AttendancePolicy.Rule rule = policy.ruleFor(userId, storeId, date);
            record = new AttendanceRecord();
            record.setUserId(userId);
            record.setAttendanceDate(date);
            record.setStoreId(storeId);
            record.setShiftId(rule.shiftId());
            record.setCreatedAt(LocalDateTime.now());
        }
        Expected expected = expected(record);
        if (checkIn != null) record.setCheckInTime(checkIn.truncatedTo(ChronoUnit.SECONDS));
        if (checkOut != null) record.setCheckOutTime(checkOut.truncatedTo(ChronoUnit.SECONDS));
        validateTimes(date, record.getCheckInTime(), record.getCheckOutTime());
        recompute(record, expected);
        record.setSource("EXCUSE");
        record.setUpdatedAt(LocalDateTime.now());
        record = saveUnique(record);
        writeExpected(record.getId(), expected.start(), expected.end());
        log(record.getId(), "EXCUSE_APPROVED", before, snapshot(record), reason, actorId);
        return record;
    }

    static void validateTimes(LocalDate date, LocalDateTime in, LocalDateTime out) {
        if (in != null && !in.toLocalDate().equals(date))
            throw BusinessException.badRequest("Giờ vào phải thuộc ngày chấm công");
        // Ca qua đêm: giờ ra có thể sang sáng hôm sau.
        if (out != null && (out.toLocalDate().isBefore(date) || out.isAfter(date.plusDays(1).atTime(12, 0))))
            throw BusinessException.badRequest("Giờ ra phải trong ngày chấm công (hoặc trước 12:00 hôm sau với ca đêm)");
        if (in != null && out != null && !out.isAfter(in)) throw BusinessException.badRequest("Giờ ra phải sau giờ vào");
    }

    private record Expected(java.time.LocalTime start, java.time.LocalTime end) {
    }

    /** Giờ dự kiến đã lưu của bản ghi (đọc JDBC); bản ghi mới / thiếu thì lấy theo cấu hình hiện hành. */
    private Expected expected(AttendanceRecord record) {
        List<Expected> rows = record.getId() == null ? List.of() : jdbc.query(
                "select expected_start, expected_end from attendance_records where id = :id and expected_start is not null and expected_end is not null",
                new MapSqlParameterSource("id", record.getId()), (rs, i) -> new Expected(rs.getObject(1, java.time.LocalTime.class),
                        rs.getObject(2, java.time.LocalTime.class)));
        if (!rows.isEmpty()) return rows.getFirst();
        AttendancePolicy.Rule rule = policy.ruleFor(record.getUserId(), record.getStoreId(), record.getAttendanceDate());
        return new Expected(rule.start(), rule.end());
    }

    private void writeExpected(UUID id, java.time.LocalTime start, java.time.LocalTime end) {
        jdbc.update("update attendance_records set expected_start = :s, expected_end = :e where id = :id",
                new MapSqlParameterSource("id", id).addValue("s", start).addValue("e", end));
    }

    /** Tính lại trễ / sớm / trạng thái: giờ dự kiến giữ theo bản ghi, ngưỡng theo cấu hình hiện hành. */
    private void recompute(AttendanceRecord record, Expected expected) {
        AttendancePolicy.Rule current = policy.ruleFor(record.getUserId(), record.getStoreId(), record.getAttendanceDate());
        AttendancePolicy.Rule rule = new AttendancePolicy.Rule(record.getShiftId(), expected.start(), expected.end(),
                current.lateThreshold(), current.earlyThreshold());
        record.setLateMinutes(record.getCheckInTime() == null ? 0 : rule.lateMinutes(record.getCheckInTime().toLocalTime()));
        boolean sameDayOut = record.getCheckOutTime() != null
                && record.getCheckOutTime().toLocalDate().equals(record.getAttendanceDate());
        record.setEarlyMinutes(sameDayOut ? rule.earlyMinutes(record.getCheckOutTime().toLocalTime()) : 0);
        record.setStatus(record.getCheckInTime() == null ? MISSING_CHECK_IN
                : record.getCheckOutTime() == null ? MISSING_CHECK_OUT : COMPLETED);
    }

    private record Location(UUID storeId, Integer distance) {
    }

    /**
     * Nhân viên cửa hàng: phải ở trong bán kính cửa hàng được phân công (khi bật yêu cầu định vị). Nhân viên Global:
     * làm ở bất kỳ đâu / tại nhà, chỉ lưu vị trí thực tế (và cửa hàng nếu chọn).
     */
    private Location verifyLocation(UUID userId, CheckRequest request) {
        UUID assigned = access.storeOf(userId);
        boolean hasPoint = request != null && request.latitude() != null && request.longitude() != null;
        if (assigned == null) {
            UUID chosen = request == null ? null : request.storeId();
            if (chosen == null) return new Location(null, null);
            AttendancePolicy.StoreLocation store = policy.storeLocation(chosen);
            if (store == null) throw BusinessException.badRequest("Cửa hàng không tồn tại");
            Integer distance = hasPoint && store.latitude() != null
                    ? AttendancePolicy.distanceMeters(request.latitude(), request.longitude(), store.latitude(), store.longitude()) : null;
            return new Location(chosen, distance);
        }
        AttendancePolicy.Settings settings = policy.settings();
        AttendancePolicy.StoreLocation store = policy.storeLocation(assigned);
        if (!hasPoint) {
            if (settings.requireStoreLocation())
                throw BusinessException.badRequest("Cần bật định vị trên thiết bị để chấm công tại cửa hàng " + store.name());
            return new Location(assigned, null);
        }
        if (store.latitude() == null || store.longitude() == null) return new Location(assigned, null);
        int distance = AttendancePolicy.distanceMeters(request.latitude(), request.longitude(), store.latitude(), store.longitude());
        if (settings.requireStoreLocation() && distance > settings.storeRadiusMeters())
            throw BusinessException.forbidden("Bạn đang cách cửa hàng " + store.name() + " khoảng " + distance
                    + " m (cho phép tối đa " + settings.storeRadiusMeters() + " m). Hãy chấm công tại cửa hàng.");
        return new Location(assigned, distance);
    }

    private AttendanceRecord saveUnique(AttendanceRecord record) {
        try {
            return records.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("Bản ghi chấm công ngày này vừa được tạo, vui lòng tải lại");
        }
    }

    JsonNode snapshot(AttendanceRecord r) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("checkIn", r.getCheckInTime() == null ? null : r.getCheckInTime().toString());
        data.put("checkOut", r.getCheckOutTime() == null ? null : r.getCheckOutTime().toString());
        data.put("status", r.getStatus());
        data.put("lateMinutes", r.getLateMinutes());
        data.put("earlyMinutes", r.getEarlyMinutes());
        data.put("source", r.getSource());
        return json.valueToTree(data);
    }

    void log(UUID recordId, String action, JsonNode before, JsonNode after, String reason, UUID actorId) {
        AttendanceAuditLog log = new AttendanceAuditLog();
        log.setAttendanceRecordId(recordId);
        log.setAction(action);
        log.setOldData(before);
        log.setNewData(after);
        log.setReason(reason);
        log.setActorId(actorId);
        log.setCreatedAt(LocalDateTime.now());
        auditLogs.save(log);
    }

    Record record(UUID id) {
        List<Record> rows = jdbc.query(SELECT + " where r.id = :id", new MapSqlParameterSource("id", id).addValue("today", LocalDate.now()),
                AttendanceRecordService::map);
        if (rows.isEmpty()) throw BusinessException.notFound("Bản ghi chấm công không tồn tại");
        return rows.getFirst();
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw BusinessException.badRequest("Cần chọn khoảng ngày");
        if (to.isBefore(from)) throw BusinessException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        if (ChronoUnit.DAYS.between(from, to) > 366) throw BusinessException.badRequest("Khoảng ngày tối đa 1 năm");
    }

    static final String SELECT = """
            select r.*, u.employee_code, u.full_name, s.name as store_name,
                   case when exists (select 1 from store_staffs x where x.user_id = r.user_id and x.active) then 'STORE' else 'GLOBAL' end
                       as employee_type,
                   exists (select 1 from attendance_locks l where l.user_id = r.user_id
                           and l.period_month = cast(date_trunc('month', r.attendance_date) as date)) as locked,
                   (r.status = 'MISSING_CHECK_OUT' and r.attendance_date = :today) as working
            from attendance_records r
            join users u on u.id = r.user_id
            left join stores s on s.id = r.store_id
            """;

    static Record map(ResultSet rs, int i) throws SQLException {
        LocalDateTime in = DbTimes.fromDb(rs.getTimestamp("check_in_time"));
        LocalDateTime out = DbTimes.fromDb(rs.getTimestamp("check_out_time"));
        String status = rs.getBoolean("working") ? "WORKING" : rs.getString("status");
        return new Record(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("employee_code"),
                rs.getString("full_name"), rs.getString("employee_type"), rs.getObject("store_id", UUID.class),
                rs.getString("store_name"), rs.getObject("attendance_date", LocalDate.class),
                rs.getObject("expected_start", java.time.LocalTime.class), rs.getObject("expected_end", java.time.LocalTime.class),
                in, out, rs.getBigDecimal("check_in_location_lat"), rs.getBigDecimal("check_in_location_lon"),
                rs.getBigDecimal("check_out_location_lat"), rs.getBigDecimal("check_out_location_lon"),
                (Integer) rs.getObject("check_in_distance_m"), (Integer) rs.getObject("check_out_distance_m"),
                in != null && out != null ? Duration.between(in, out).toMinutes() : null, rs.getInt("late_minutes"),
                rs.getInt("early_minutes"), status, rs.getString("source"), rs.getString("notes"), rs.getBoolean("locked"));
    }
}
