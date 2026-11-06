package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Dashboard;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Holiday;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Lookups;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Settings;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.StoreRef;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Danh mục, dashboard HR, cấu hình giờ làm / ngày nghỉ tuần / ngày lễ. */
@Service
@Transactional(readOnly = true)
public class AttendanceAdminService {
    /** Nhân viên đang làm việc (tài khoản nội bộ còn hoạt động). */
    static final String ACTIVE_EMPLOYEE = "u.active and u.deleted_at is null and coalesce(u.employment_status, 'ACTIVE') = 'ACTIVE'";

    private final NamedParameterJdbcTemplate jdbc;
    private final AttendanceAccess access;
    private final LeaveRequestService leaves;

    public AttendanceAdminService(NamedParameterJdbcTemplate jdbc, AttendanceAccess access, LeaveRequestService leaves) {
        this.jdbc = jdbc;
        this.access = access;
        this.leaves = leaves;
    }

    public Lookups lookups(Actor actor) {
        List<StoreRef> stores = jdbc.query("select id, name from stores where active and (:global or id = :store) order by name",
                new MapSqlParameterSource("global", actor.global()).addValue("store", actor.storeId(), java.sql.Types.OTHER),
                (rs, i) -> new StoreRef(rs.getObject("id", UUID.class), rs.getString("name")));
        return new Lookups(actor.permissions(), actor.storeId(), actor.storeName(), stores, leaves.types(false));
    }

    /** Nhân viên đang làm việc trong phạm vi người xem (tối đa 50, lọc theo mã / tên). */
    public List<AttendanceDtos.EmployeeRef> employees(Actor actor, String keyword, UUID storeId) {
        String kw = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
        return jdbc.query("""
                select u.id, u.employee_code, u.full_name, ss.store_id, s.name as store_name
                from users u
                left join store_staffs ss on ss.user_id = u.id and ss.active
                left join stores s on s.id = ss.store_id
                where %s and %s
                  and (cast(:kw as varchar) is null or lower(u.full_name) like :kw or lower(coalesce(u.employee_code, '')) like :kw)
                order by u.full_name limit 50
                """.formatted(ACTIVE_EMPLOYEE, AttendanceAccess.guard("u.id", "cast(null as uuid)")),
                AttendanceAccess.params(actor, access.storeFilter(actor, storeId), null).addValue("kw", kw, java.sql.Types.VARCHAR),
                (rs, i) -> new AttendanceDtos.EmployeeRef(rs.getObject("id", UUID.class), rs.getString("employee_code"),
                        rs.getString("full_name"), rs.getObject("store_id", UUID.class), rs.getString("store_name")));
    }

    public Dashboard dashboard(Actor actor, LocalDate date, UUID storeId) {
        LocalDate day = date == null ? LocalDate.now() : date;
        MapSqlParameterSource params = AttendanceAccess.params(actor, access.storeFilter(actor, storeId), null)
                .addValue("d", day).addValue("today", LocalDate.now()).addValue("nowTime", LocalTime.now());
        String guard = AttendanceAccess.guard("u.id", "cast(null as uuid)");
        return jdbc.queryForObject("""
                with emp as (select u.id from users u where %s and %s),
                rec as (select r.* from attendance_records r join emp on emp.id = r.user_id where r.attendance_date = :d),
                lv as (select distinct l.user_id from leave_requests l join emp on emp.id = l.user_id
                       where l.status = 'APPROVED' and :d between l.start_date and l.end_date),
                -- Ca đã công bố trong ngày (theo cửa hàng của ca) để phát hiện vắng / chưa vào ca.
                sch as (select s.start_time, s.end_time,
                               exists (select 1 from attendance_records r where r.user_id = s.user_id and r.attendance_date = s.work_date
                                       and r.check_in_time is not null) as checked_in,
                               exists (select 1 from attendance_records r where r.user_id = s.user_id and r.attendance_date = s.work_date)
                                   as recorded,
                               exists (select 1 from leave_requests l where l.user_id = s.user_id and l.status = 'APPROVED'
                                       and s.work_date between l.start_date and l.end_date) as on_leave
                        from shift_schedules s
                        where s.work_date = :d and s.status = 'PUBLISHED' and %s)
                select (select count(*) from emp) as total,
                       (select count(*) from rec where check_in_time is not null) as checked_in,
                       (select count(*) from rec where late_minutes > 0) as late,
                       (select count(*) from rec where early_minutes > 0) as early,
                       (select count(*) from rec where check_out_time is not null) as checked_out,
                       (select count(*) from rec where check_in_time is not null and check_out_time is null
                          and (:d < :today or (:d = :today and :nowTime > coalesce(expected_end, time '18:00')))) as missing_out,
                       (select count(*) from lv) as on_leave,
                       (select count(*) from emp e where not exists (select 1 from rec where rec.user_id = e.id and rec.check_in_time is not null)
                          and not exists (select 1 from lv where lv.user_id = e.id)) as not_checked_in,
                       (select count(*) from excuse_requests x join users u on u.id = x.user_id where x.status = 'PENDING' and %s) as p_excuse,
                       (select count(*) from leave_requests x join users u on u.id = x.user_id where x.status = 'PENDING' and %s) as p_leave,
                       (select count(*) from overtime_requests x join users u on u.id = x.user_id where x.status = 'PENDING' and %s) as p_ot,
                       (select count(*) from sch) as scheduled,
                       (select count(*) from sch where not sch.checked_in and not sch.on_leave and not sch.recorded
                          and (:d < :today or (:d = :today and sch.end_time > sch.start_time and :nowTime >= sch.end_time))) as absent,
                       (select count(*) from sch where not sch.checked_in and not sch.on_leave and not sch.recorded and :d = :today
                          and :nowTime >= sch.start_time and (sch.end_time < sch.start_time or :nowTime < sch.end_time)) as not_yet_in,
                       (select count(*) from shift_swap_requests w where w.status = 'PENDING_APPROVAL'
                          and (:global or w.store_id = :scopeStore) and (cast(:store as uuid) is null or w.store_id = :store)) as p_swap
                """.formatted(ACTIVE_EMPLOYEE, guard, AttendanceAccess.guard("s.user_id", "s.store_id"),
                AttendanceAccess.guard("x.user_id", "x.store_id"), AttendanceAccess.guard("x.user_id", "x.store_id"),
                AttendanceAccess.guard("x.user_id", "x.store_id")),
                params, (rs, i) -> new Dashboard(day, rs.getInt("total"), rs.getInt("checked_in"), rs.getInt("not_checked_in"),
                        rs.getInt("late"), rs.getInt("early"), rs.getInt("checked_out"), rs.getInt("missing_out"),
                        rs.getInt("on_leave"), rs.getInt("p_excuse"), rs.getInt("p_leave"), rs.getInt("p_ot"),
                        rs.getInt("scheduled"), rs.getInt("absent"), rs.getInt("not_yet_in"), rs.getInt("p_swap")));
    }

    // ------------------------------------------------------------------ cấu hình

    public Settings settings() {
        return jdbc.queryForObject("""
                select array_to_string(s.weekly_off_days, ',') as off_days, s.standard_hours_per_day, s.store_radius_meters,
                       s.require_store_location, c.check_in_time, c.check_out_time, c.late_threshold_minutes,
                       c.early_departure_threshold_minutes
                from attendance_settings s
                left join attendance_configs c on c.store_id is null and c.shift_id is null
                where s.id = 1
                """, new MapSqlParameterSource(), (rs, i) -> new Settings(
                java.util.Arrays.stream(rs.getString("off_days").split(",")).filter(v -> !v.isBlank()).map(Integer::valueOf).toList(),
                rs.getBigDecimal("standard_hours_per_day"), rs.getInt("store_radius_meters"), rs.getBoolean("require_store_location"),
                rs.getObject("check_in_time", LocalTime.class), rs.getObject("check_out_time", LocalTime.class),
                rs.getInt("late_threshold_minutes"), rs.getInt("early_departure_threshold_minutes")));
    }

    @Transactional
    public Settings updateSettings(Actor actor, Settings request) {
        requireGlobalAdmin(actor);
        if (!request.defaultCheckOut().isAfter(request.defaultCheckIn()))
            throw BusinessException.badRequest("Giờ ra mặc định phải sau giờ vào");
        if (request.weeklyOffDays().stream().distinct().count() >= 7)
            throw BusinessException.badRequest("Phải có ít nhất một ngày làm việc trong tuần");
        jdbc.update("""
                update attendance_settings set weekly_off_days = cast(:off as smallint[]), standard_hours_per_day = :hours,
                       store_radius_meters = :radius, require_store_location = :requireLocation, updated_by = :actor,
                       updated_at = now() at time zone 'UTC'
                where id = 1
                """, new MapSqlParameterSource("off", "{" + String.join(",", request.weeklyOffDays().stream().distinct().sorted()
                .map(String::valueOf).toList()) + "}")
                .addValue("hours", request.standardHoursPerDay()).addValue("radius", request.storeRadiusMeters())
                .addValue("requireLocation", request.requireStoreLocation()).addValue("actor", actor.userId()));
        jdbc.update("""
                update attendance_configs set check_in_time = :in, check_out_time = :out, late_threshold_minutes = :late,
                       early_departure_threshold_minutes = :early, updated_at = now() at time zone 'UTC'
                where store_id is null and shift_id is null
                """, new MapSqlParameterSource("in", request.defaultCheckIn()).addValue("out", request.defaultCheckOut())
                .addValue("late", request.lateThresholdMinutes()).addValue("early", request.earlyThresholdMinutes()));
        return settings();
    }

    public List<Holiday> holidays(int year) {
        return jdbc.query("select holiday_date, name from public_holidays where extract(year from holiday_date) = :y order by holiday_date",
                new MapSqlParameterSource("y", year), (rs, i) -> new Holiday(rs.getObject(1, LocalDate.class), rs.getString(2)));
    }

    @Transactional
    public List<Holiday> addHoliday(Actor actor, Holiday holiday) {
        requireGlobalAdmin(actor);
        requireMonthOpen(holiday.date());
        jdbc.update("""
                insert into public_holidays(holiday_date, name, created_by) values (:d, :name, :actor)
                on conflict (holiday_date) do update set name = excluded.name
                """, new MapSqlParameterSource("d", holiday.date()).addValue("name", holiday.name().trim()).addValue("actor", actor.userId()));
        return holidays(holiday.date().getYear());
    }

    @Transactional
    public List<Holiday> deleteHoliday(Actor actor, LocalDate date) {
        requireGlobalAdmin(actor);
        requireMonthOpen(date);
        jdbc.update("delete from public_holidays where holiday_date = :d", new MapSqlParameterSource("d", date));
        return holidays(date.getYear());
    }

    /** Ngày lễ ảnh hưởng ngày công chuẩn, nên không đổi được khi đã có kỳ lương tháng đó gửi duyệt. */
    private void requireMonthOpen(LocalDate date) {
        Boolean locked = jdbc.queryForObject("select exists (select 1 from attendance_locks where period_month = :m)",
                new MapSqlParameterSource("m", date.withDayOfMonth(1)), Boolean.class);
        if (Boolean.TRUE.equals(locked)) throw BusinessException.conflict("Tháng này đã có kỳ lương chốt công, không đổi được ngày lễ");
    }

    private static void requireGlobalAdmin(Actor actor) {
        if (!actor.globalAdmin()) throw BusinessException.forbidden("Chỉ quản trị chấm công cấp chuỗi được thay đổi cấu hình");
    }
}
