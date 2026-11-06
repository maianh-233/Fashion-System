package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Availability;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.AvailabilityRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.BulkResult;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.CopyWeekRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Entry;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.EntryRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Grid;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.GridDay;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.GridEmployee;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.GridStats;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.LeaveDay;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.PublishRequest;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Template;
import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch làm theo ngày: quản lý cửa hàng xếp ca (nháp) → công bố cho nhân viên; sao chép tuần; đối chiếu với chấm công
 * (đi trễ / về sớm / vắng). Nhân viên xem lịch đã công bố và đăng ký ngày rảnh / bận. Lịch đã công bố là giờ làm dự
 * kiến của {@link AttendancePolicy}.
 */
@Service
@Transactional(readOnly = true)
public class ShiftScheduleService {
    /** Giới hạn giờ làm thường / tuần theo Bộ luật Lao động — chỉ cảnh báo trên lịch, không chặn. */
    static final int WEEKLY_HOUR_LIMIT = 48;
    static final int MAX_DAYS_AHEAD = 90;
    static final int AVAILABILITY_DAYS_AHEAD = 60;
    static final String DRAFT = "DRAFT";
    static final String PUBLISHED = "PUBLISHED";
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");

    private final NamedParameterJdbcTemplate jdbc;
    private final AttendanceAccess access;
    private final AttendanceLockService locks;
    private final WorkCalendar calendar;
    private final ShiftTemplateService templates;
    private final ShiftNotifier notifier;

    public ShiftScheduleService(NamedParameterJdbcTemplate jdbc, AttendanceAccess access, AttendanceLockService locks,
                                WorkCalendar calendar, ShiftTemplateService templates, ShiftNotifier notifier) {
        this.jdbc = jdbc;
        this.access = access;
        this.locks = locks;
        this.calendar = calendar;
        this.templates = templates;
        this.notifier = notifier;
    }

    // ------------------------------------------------------------------ lịch tuần (quản lý)

    public Grid grid(Actor actor, UUID storeId, LocalDate anyDayOfWeek) {
        UUID store = requireStore(actor, storeId);
        LocalDate from = (anyDayOfWeek == null ? LocalDate.now() : anyDayOfWeek).with(DayOfWeek.MONDAY);
        LocalDate to = from.plusDays(6);
        MapSqlParameterSource p = new MapSqlParameterSource("store", store).addValue("from", from).addValue("to", to);
        String storeName = jdbc.queryForObject("select name from stores where id = :store", p, String.class);

        // Nhân viên của cửa hàng + người ngoài (nhân viên Global / cũ) đã có ca tại cửa hàng trong tuần.
        record Emp(UUID id, String code, String name, boolean member, boolean hasStore) {
        }
        List<Emp> emps = jdbc.query("""
                select u.id, u.employee_code, u.full_name,
                       exists (select 1 from store_staffs x where x.user_id = u.id and x.active and x.store_id = :store) as member,
                       exists (select 1 from store_staffs x where x.user_id = u.id and x.active) as has_store
                from users u
                where (%s and exists (select 1 from store_staffs x where x.user_id = u.id and x.active and x.store_id = :store))
                   or exists (select 1 from shift_schedules s where s.user_id = u.id and s.store_id = :store
                              and s.work_date between :from and :to)
                order by 4 desc, u.full_name
                """.formatted(AttendanceAdminService.ACTIVE_EMPLOYEE), p,
                (rs, i) -> new Emp(rs.getObject("id", UUID.class), rs.getString("employee_code"), rs.getString("full_name"),
                        rs.getBoolean("member"), rs.getBoolean("has_store")));
        List<UUID> ids = emps.stream().map(Emp::id).toList();
        p.addValue("ids", ids.isEmpty() ? List.of(new UUID(0, 0)) : ids);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<Entry> entries = jdbc.query(ENTRY_SELECT + """
                 where s.work_date between :from and :to and (s.store_id = :store or s.user_id in (:ids))
                 order by s.work_date, s.start_time, u.full_name
                """, p, entryMapper(today, now, store));

        List<Availability> availability = jdbc.query(AVAILABILITY_SELECT + " where a.user_id in (:ids) and a.work_date between :from and :to",
                p, ShiftScheduleService::mapAvailability);
        List<LeaveDay> leaves = leaveDays(p, from, to);
        List<Template> shiftTemplates = templates.list(actor, store, false);

        WorkCalendar.Snapshot cal = calendar.snapshot(from, to);
        List<GridDay> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            LocalDate day = d;
            List<Entry> atStore = entries.stream().filter(e -> e.date().equals(day) && store.equals(e.storeId())).toList();
            days.add(new GridDay(d, cal.holidays().get(d), cal.weeklyOff(d), atStore.size(),
                    atStore.stream().mapToInt(Entry::paidMinutes).sum()));
        }
        List<GridEmployee> employees = emps.stream().map(e -> {
            List<Entry> mine = entries.stream().filter(x -> x.userId().equals(e.id())).toList();
            return new GridEmployee(e.id(), e.code(), e.name(), e.hasStore() ? "STORE" : "GLOBAL", e.member(),
                    mine.stream().mapToInt(Entry::paidMinutes).sum(), mine.size());
        }).toList();

        List<Entry> atStore = entries.stream().filter(e -> store.equals(e.storeId())).toList();
        Integer pendingSwaps = jdbc.queryForObject(
                "select count(*) from shift_swap_requests where store_id = :store and status = 'PENDING_APPROVAL'", p, Integer.class);
        GridStats stats = new GridStats(atStore.size(), (int) atStore.stream().filter(e -> DRAFT.equals(e.status())).count(),
                (int) atStore.stream().filter(e -> PUBLISHED.equals(e.status())).count(),
                atStore.stream().mapToInt(Entry::paidMinutes).sum(),
                (int) atStore.stream().filter(e -> "ABSENT".equals(e.actualStatus())).count(),
                (int) atStore.stream().filter(e -> e.lateMinutes() > 0).count(),
                (int) atStore.stream().filter(e -> e.earlyMinutes() > 0).count(),
                pendingSwaps == null ? 0 : pendingSwaps);
        return new Grid(store, storeName, from, to, days, employees, entries, availability, leaves, shiftTemplates, stats,
                WEEKLY_HOUR_LIMIT);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Entry create(Actor actor, EntryRequest request) {
        UUID store = requireStore(actor, request.storeId());
        requireEligible(request.userId(), store);
        requireSchedulable(request.userId(), request.date(), null);
        Resolved shift = resolve(request, store);
        UUID id = jdbc.queryForObject("""
                insert into shift_schedules(user_id, store_id, work_date, shift_id, shift_name, color, start_time, end_time,
                                            break_minutes, status, note, created_by, created_at)
                values (:user, :store, :date, cast(:shift as uuid), :name, :color, :start, :end, :break, 'DRAFT', :note, :actor,
                        now() at time zone 'UTC')
                returning id
                """, shift.params().addValue("user", request.userId()).addValue("store", store).addValue("date", request.date())
                .addValue("note", RequestQueries.trim(request.note())).addValue("actor", actor.userId()), UUID.class);
        return entry(id, store);
    }

    /** Sửa / kéo thả ca sang ngày khác hoặc người khác (cùng cửa hàng). Ca đã công bố vẫn ở trạng thái công bố. */
    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Entry update(Actor actor, UUID id, EntryRequest request) {
        Current current = current(id);
        UUID store = requireStore(actor, current.storeId());
        requireChangeable(current);
        if (!current.userId().equals(request.userId())) requireEligible(request.userId(), store);
        requireSchedulable(request.userId(), request.date(), id);
        Resolved shift = resolve(request, store);
        jdbc.update("""
                update shift_schedules set user_id = :user, work_date = :date, shift_id = cast(:shift as uuid), shift_name = :name,
                       color = :color, start_time = :start, end_time = :end, break_minutes = :break, note = :note,
                       updated_by = :actor, updated_at = now() at time zone 'UTC'
                where id = :id
                """, shift.params().addValue("user", request.userId()).addValue("date", request.date())
                .addValue("note", RequestQueries.trim(request.note())).addValue("actor", actor.userId()).addValue("id", id));
        cancelOpenSwaps(id, "Lịch ca đã được quản lý thay đổi");
        if (PUBLISHED.equals(current.status())) {
            String when = shift.label() + " ngày " + request.date().format(DM);
            if (current.userId().equals(request.userId())) {
                notifier.toUser(request.userId(), actor.userId(), "Lịch làm thay đổi",
                        "Ca ngày " + current.date().format(DM) + " của bạn đã đổi thành " + when + ".", id);
            } else {
                notifier.toUser(current.userId(), actor.userId(), "Ca làm bị hủy",
                        "Ca " + current.label() + " ngày " + current.date().format(DM) + " của bạn đã được chuyển cho người khác.", id);
                notifier.toUser(request.userId(), actor.userId(), "Bạn có ca làm mới", "Bạn được xếp " + when + ".", id);
            }
        }
        return entry(id, store);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public void delete(Actor actor, UUID id) {
        Current current = current(id);
        requireStore(actor, current.storeId());
        requireChangeable(current);
        cancelOpenSwaps(id, "Ca đã bị xóa khỏi lịch");
        jdbc.update("delete from shift_schedules where id = :id", new MapSqlParameterSource("id", id));
        if (PUBLISHED.equals(current.status()))
            notifier.toUser(current.userId(), actor.userId(), "Ca làm bị hủy",
                    "Ca " + current.label() + " ngày " + current.date().format(DM) + " của bạn đã bị hủy.", id);
    }

    /** Công bố mọi ca nháp (từ hôm nay trở đi) của cửa hàng trong khoảng ngày và báo cho từng nhân viên. */
    @Transactional
    @BusinessAudit("ATTENDANCE")
    public BulkResult publish(Actor actor, PublishRequest request) {
        UUID store = requireStore(actor, request.storeId());
        requireRange(request.from(), request.to(), 62);
        LocalDate from = request.from().isBefore(LocalDate.now()) ? LocalDate.now() : request.from();
        record Row(UUID id, UUID userId) {
        }
        List<Row> rows = jdbc.query("""
                update shift_schedules set status = 'PUBLISHED', published_at = now() at time zone 'UTC', updated_by = :actor,
                       updated_at = now() at time zone 'UTC'
                where store_id = :store and status = 'DRAFT' and work_date between :from and :to
                returning id, user_id
                """, new MapSqlParameterSource("store", store).addValue("from", from).addValue("to", request.to())
                .addValue("actor", actor.userId()), (rs, i) -> new Row(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)));
        Map<UUID, Integer> perUser = new LinkedHashMap<>();
        rows.forEach(r -> perUser.merge(r.userId(), 1, Integer::sum));
        String range = request.from().format(DM) + " – " + request.to().format(DM);
        perUser.forEach((user, count) -> notifier.toUser(user, actor.userId(), "Lịch làm đã công bố",
                "Lịch làm " + range + " của bạn đã được công bố (" + count + " ca mới). Xem chi tiết trong Lịch làm của tôi.", null));
        return new BulkResult(rows.size(), 0, List.of());
    }

    /** Sao chép ca của một tuần sang tuần khác (thành ca nháp); bỏ qua ngày đã qua, người đã có ca, đang nghỉ phép. */
    @Transactional
    @BusinessAudit("ATTENDANCE")
    public BulkResult copyWeek(Actor actor, CopyWeekRequest request) {
        UUID store = requireStore(actor, request.storeId());
        LocalDate source = request.sourceWeek().with(DayOfWeek.MONDAY);
        LocalDate target = request.targetWeek().with(DayOfWeek.MONDAY);
        if (source.equals(target)) throw BusinessException.badRequest("Tuần nguồn và tuần đích phải khác nhau");
        long shiftDays = ChronoUnit.DAYS.between(source, target);
        record Src(UUID userId, String name, LocalDate date, UUID shiftId, String shiftName, String color, LocalTime start,
                   LocalTime end, int breakMinutes, String note) {
        }
        List<Src> sources = jdbc.query("""
                select s.user_id, u.full_name, s.work_date, s.shift_id, s.shift_name, s.color, s.start_time, s.end_time,
                       s.break_minutes, s.note
                from shift_schedules s join users u on u.id = s.user_id
                where s.store_id = :store and s.work_date between :from and :to order by s.work_date, u.full_name
                """, new MapSqlParameterSource("store", store).addValue("from", source).addValue("to", source.plusDays(6)),
                (rs, i) -> new Src(rs.getObject("user_id", UUID.class), rs.getString("full_name"),
                        rs.getObject("work_date", LocalDate.class), rs.getObject("shift_id", UUID.class), rs.getString("shift_name"),
                        rs.getString("color"), rs.getObject("start_time", LocalTime.class), rs.getObject("end_time", LocalTime.class),
                        rs.getInt("break_minutes"), rs.getString("note")));
        if (sources.isEmpty()) throw BusinessException.badRequest("Tuần nguồn chưa có ca nào để sao chép");
        int created = 0;
        List<String> skipped = new ArrayList<>();
        for (Src s : sources) {
            LocalDate date = s.date().plusDays(shiftDays);
            try {
                requireEligible(s.userId(), store);
                requireSchedulable(s.userId(), date, null);
            } catch (BusinessException e) {
                skipped.add(s.name() + " " + date.format(DM) + ": " + e.getMessage());
                continue;
            }
            jdbc.update("""
                    insert into shift_schedules(user_id, store_id, work_date, shift_id, shift_name, color, start_time, end_time,
                                                break_minutes, status, note, created_by, created_at)
                    values (:user, :store, :date, cast(:shift as uuid), :name, :color, :start, :end, :break, 'DRAFT', :note, :actor,
                            now() at time zone 'UTC')
                    """, new MapSqlParameterSource("user", s.userId()).addValue("store", store).addValue("date", date)
                    .addValue("shift", s.shiftId(), java.sql.Types.OTHER).addValue("name", s.shiftName()).addValue("color", s.color())
                    .addValue("start", s.start()).addValue("end", s.end()).addValue("break", s.breakMinutes())
                    .addValue("note", s.note()).addValue("actor", actor.userId()));
            created++;
        }
        return new BulkResult(created, skipped.size(), skipped);
    }

    // ------------------------------------------------------------------ nhân viên

    /** Ca đã công bố của chính mình (mọi cửa hàng). */
    public List<Entry> mine(UUID userId, LocalDate from, LocalDate to) {
        requireRange(from, to, 62);
        return jdbc.query(ENTRY_SELECT + """
                 where s.user_id = :user and s.status = 'PUBLISHED' and s.work_date between :from and :to
                 order by s.work_date
                """, new MapSqlParameterSource("user", userId).addValue("from", from).addValue("to", to),
                entryMapper(LocalDate.now(), LocalTime.now(), null));
    }

    public List<Availability> availability(UUID userId, LocalDate from, LocalDate to) {
        requireRange(from, to, 92);
        return jdbc.query(AVAILABILITY_SELECT + " where a.user_id = :user and a.work_date between :from and :to order by a.work_date",
                new MapSqlParameterSource("user", userId).addValue("from", from).addValue("to", to),
                ShiftScheduleService::mapAvailability);
    }

    @Transactional
    public Availability setAvailability(UUID userId, AvailabilityRequest request) {
        String kind = request.kind().trim().toUpperCase();
        if (!Set.of("AVAILABLE", "UNAVAILABLE").contains(kind)) throw BusinessException.badRequest("Loại đăng ký không hợp lệ");
        long offset = ChronoUnit.DAYS.between(LocalDate.now(), request.date());
        if (offset < 0 || offset > AVAILABILITY_DAYS_AHEAD)
            throw BusinessException.badRequest("Chỉ đăng ký cho hôm nay đến " + AVAILABILITY_DAYS_AHEAD + " ngày tới");
        UUID preferred = "AVAILABLE".equals(kind) ? request.preferredShiftId() : null;
        if (preferred != null) {
            Template t = templates.get(preferred);
            UUID home = access.storeOf(userId);
            if (!t.active() || (t.storeId() != null && !t.storeId().equals(home)))
                throw BusinessException.badRequest("Ca mong muốn không áp dụng cho cửa hàng của bạn");
        }
        MapSqlParameterSource p = new MapSqlParameterSource("user", userId).addValue("date", request.date()).addValue("kind", kind)
                .addValue("shift", preferred, java.sql.Types.OTHER).addValue("note", RequestQueries.trim(request.note()));
        jdbc.update("""
                insert into shift_availabilities(user_id, work_date, kind, preferred_shift_id, note, updated_at)
                values (:user, :date, :kind, cast(:shift as uuid), :note, now() at time zone 'UTC')
                on conflict (user_id, work_date) do update set kind = excluded.kind, preferred_shift_id = excluded.preferred_shift_id,
                    note = excluded.note, updated_at = excluded.updated_at
                """, p);
        return jdbc.queryForObject(AVAILABILITY_SELECT + " where a.user_id = :user and a.work_date = :date", p,
                ShiftScheduleService::mapAvailability);
    }

    @Transactional
    public void clearAvailability(UUID userId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) throw BusinessException.badRequest("Không sửa đăng ký của ngày đã qua");
        jdbc.update("delete from shift_availabilities where user_id = :user and work_date = :date",
                new MapSqlParameterSource("user", userId).addValue("date", date));
    }

    // ------------------------------------------------------------------ dùng chung (cả đổi ca)

    /** Cửa hàng của lịch: bắt buộc chọn; nhân viên cửa hàng chỉ thao tác cửa hàng mình. */
    UUID requireStore(Actor actor, UUID storeId) {
        UUID store = access.storeFilter(actor, storeId);
        if (store == null) throw BusinessException.badRequest("Chọn cửa hàng để xếp ca");
        Boolean exists = jdbc.queryForObject("select exists (select 1 from stores where id = :id)",
                new MapSqlParameterSource("id", store), Boolean.class);
        if (!Boolean.TRUE.equals(exists)) throw BusinessException.notFound("Cửa hàng không tồn tại");
        return store;
    }

    /** Nhân viên đang làm việc, thuộc cửa hàng này hoặc là nhân viên Global (không thuộc cửa hàng nào). */
    void requireEligible(UUID userId, UUID storeId) {
        List<UUID> home = jdbc.query("""
                select (select x.store_id from store_staffs x where x.user_id = u.id and x.active
                        order by (x.store_id = :store) desc limit 1) as home
                from users u where u.id = :user and %s
                """.formatted(AttendanceAdminService.ACTIVE_EMPLOYEE),
                new MapSqlParameterSource("user", userId).addValue("store", storeId), (rs, i) -> rs.getObject(1, UUID.class));
        if (home.isEmpty()) throw BusinessException.notFound("Nhân viên không tồn tại hoặc đã nghỉ việc");
        if (home.getFirst() != null && !home.getFirst().equals(storeId))
            throw BusinessException.forbidden("Nhân viên thuộc cửa hàng khác, không xếp ca tại cửa hàng này được");
    }

    /**
     * Ngày còn xếp được cho nhân viên: không phải ngày đã qua / đã chấm công, chưa có ca khác, không nghỉ phép đã duyệt,
     * tháng chưa chốt lương. {@code excludeId} = ca đang được sửa.
     */
    void requireSchedulable(UUID userId, LocalDate date, UUID excludeId) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) throw BusinessException.badRequest("Không xếp ca cho ngày đã qua");
        if (ChronoUnit.DAYS.between(today, date) > MAX_DAYS_AHEAD)
            throw BusinessException.badRequest("Chỉ xếp ca trước tối đa " + MAX_DAYS_AHEAD + " ngày");
        locks.requireUnlocked(userId, date);
        MapSqlParameterSource p = new MapSqlParameterSource("user", userId).addValue("date", date)
                .addValue("exclude", excludeId, java.sql.Types.OTHER);
        Map<String, Object> state = jdbc.queryForMap("""
                select (select st.name from shift_schedules s join stores st on st.id = s.store_id
                        where s.user_id = :user and s.work_date = :date and (cast(:exclude as uuid) is null or s.id <> :exclude)
                        limit 1) as other_store,
                       exists (select 1 from attendance_records r where r.user_id = :user and r.attendance_date = :date) as recorded,
                       exists (select 1 from leave_requests l where l.user_id = :user and l.status = 'APPROVED'
                               and :date between l.start_date and l.end_date
                               and not ((l.is_half_day_start and l.start_date = :date) or (l.is_half_day_end and l.end_date = :date)))
                           as on_leave
                """, p);
        if (state.get("other_store") != null)
            throw BusinessException.conflict("Nhân viên đã có ca ngày " + date.format(DM) + " tại " + state.get("other_store"));
        if (Boolean.TRUE.equals(state.get("recorded")))
            throw BusinessException.conflict("Nhân viên đã chấm công ngày " + date.format(DM) + ", không đổi ca được nữa");
        if (Boolean.TRUE.equals(state.get("on_leave")))
            throw BusinessException.conflict("Nhân viên nghỉ phép (đã duyệt) ngày " + date.format(DM));
    }

    record Current(UUID id, UUID userId, UUID storeId, LocalDate date, String status, String shiftName, LocalTime start,
                   LocalTime end) {
        String label() {
            return shiftName + " (" + start + "–" + end + ")";
        }
    }

    Current current(UUID id) {
        List<Current> rows = jdbc.query("""
                select id, user_id, store_id, work_date, status, shift_name, start_time, end_time from shift_schedules where id = :id
                """, new MapSqlParameterSource("id", id), (rs, i) -> new Current(rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class), rs.getObject("store_id", UUID.class), rs.getObject("work_date", LocalDate.class),
                rs.getString("status"), rs.getString("shift_name"), rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class)));
        if (rows.isEmpty()) throw BusinessException.notFound("Ca làm không tồn tại");
        return rows.getFirst();
    }

    /** Ca đã qua hoặc đã chấm công thì giữ nguyên để đối chiếu. */
    void requireChangeable(Current current) {
        if (current.date().isBefore(LocalDate.now())) throw BusinessException.badRequest("Không sửa ca của ngày đã qua");
        Boolean recorded = jdbc.queryForObject(
                "select exists (select 1 from attendance_records where user_id = :u and attendance_date = :d)",
                new MapSqlParameterSource("u", current.userId()).addValue("d", current.date()), Boolean.class);
        if (Boolean.TRUE.equals(recorded)) throw BusinessException.conflict("Nhân viên đã chấm công cho ca này, không sửa được");
        locks.requireUnlocked(current.userId(), current.date());
    }

    void cancelOpenSwaps(UUID scheduleId, String reason) {
        jdbc.update("""
                update shift_swap_requests set status = 'CANCELLED', rejection_reason = :reason, updated_at = now() at time zone 'UTC'
                where (requester_schedule_id = :id or target_schedule_id = :id) and status in ('PENDING_PEER', 'PENDING_APPROVAL')
                """, new MapSqlParameterSource("id", scheduleId).addValue("reason", reason));
    }

    private record Resolved(UUID shiftId, String name, String color, LocalTime start, LocalTime end, int breakMinutes) {
        MapSqlParameterSource params() {
            return new MapSqlParameterSource().addValue("shift", shiftId, java.sql.Types.OTHER).addValue("name", name).addValue("color", color)
                    .addValue("start", start).addValue("end", end).addValue("break", breakMinutes);
        }

        String label() {
            return name + " (" + start + "–" + end + ")";
        }
    }

    /** Ca mẫu (giờ chụp lại tại thời điểm xếp) hoặc giờ tùy chỉnh. */
    private Resolved resolve(EntryRequest request, UUID store) {
        if (request.shiftId() != null) {
            Template t = templates.get(request.shiftId());
            if (!t.active()) throw BusinessException.badRequest("Ca mẫu " + t.name() + " đã ngừng sử dụng");
            if (t.storeId() != null && !t.storeId().equals(store))
                throw BusinessException.badRequest("Ca mẫu " + t.name() + " thuộc cửa hàng khác");
            return new Resolved(t.id(), t.name(), t.color(), t.startTime(), t.endTime(), t.breakMinutes());
        }
        if (request.startTime() == null || request.endTime() == null)
            throw BusinessException.badRequest("Chọn ca mẫu hoặc nhập giờ bắt đầu / kết thúc");
        if (request.startTime().equals(request.endTime())) throw BusinessException.badRequest("Giờ bắt đầu và kết thúc ca phải khác nhau");
        int brk = request.breakMinutes() == null ? 0 : request.breakMinutes();
        if (brk >= ShiftTemplateService.spanMinutes(request.startTime(), request.endTime()))
            throw BusinessException.badRequest("Thời gian nghỉ phải ngắn hơn độ dài ca");
        return new Resolved(null, "Ca tùy chỉnh", "zinc", request.startTime().withSecond(0).withNano(0),
                request.endTime().withSecond(0).withNano(0), brk);
    }

    Entry entry(UUID id, UUID gridStore) {
        List<Entry> rows = jdbc.query(ENTRY_SELECT + " where s.id = :id", new MapSqlParameterSource("id", id),
                entryMapper(LocalDate.now(), LocalTime.now(), gridStore));
        if (rows.isEmpty()) throw BusinessException.notFound("Ca làm không tồn tại");
        return rows.getFirst();
    }

    private List<LeaveDay> leaveDays(MapSqlParameterSource p, LocalDate from, LocalDate to) {
        List<LeaveDay> result = new ArrayList<>();
        jdbc.query("""
                select l.user_id, l.start_date, l.end_date, l.is_half_day_start, l.is_half_day_end, t.name
                from leave_requests l join leave_types t on t.id = l.leave_type_id
                where l.user_id in (:ids) and l.status = 'APPROVED' and l.start_date <= :to and l.end_date >= :from
                """, p, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            LocalDate start = rs.getObject("start_date", LocalDate.class);
            LocalDate end = rs.getObject("end_date", LocalDate.class);
            for (LocalDate d = start.isBefore(from) ? from : start; !d.isAfter(end) && !d.isAfter(to); d = d.plusDays(1)) {
                boolean half = (rs.getBoolean("is_half_day_start") && d.equals(start)) || (rs.getBoolean("is_half_day_end") && d.equals(end));
                result.add(new LeaveDay(rs.getObject("user_id", UUID.class), d, rs.getString("name"), half));
            }
        });
        return result;
    }

    static void requireRange(LocalDate from, LocalDate to, int maxDays) {
        if (from == null || to == null) throw BusinessException.badRequest("Cần chọn khoảng ngày");
        if (to.isBefore(from)) throw BusinessException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        if (ChronoUnit.DAYS.between(from, to) > maxDays) throw BusinessException.badRequest("Khoảng ngày tối đa " + maxDays + " ngày");
    }

    /**
     * Đối chiếu ca với chấm công tại thời điểm {@code today}/{@code now}. Ca qua đêm (giờ kết thúc nhỏ hơn giờ bắt đầu)
     * coi như kết thúc lúc nửa đêm khi xét vắng trong ngày.
     */
    static String actualStatus(LocalDate date, LocalTime start, LocalTime end, LocalDateTime checkIn, LocalDateTime checkOut,
                               boolean recordExists, int late, int early, boolean onLeave, LocalDate today, LocalTime now) {
        if (checkIn == null) {
            if (recordExists) return "MISSING_CHECK_IN";
            if (onLeave) return "ON_LEAVE";
            if (date.isAfter(today)) return "UPCOMING";
            if (date.isBefore(today)) return "ABSENT";
            if (now.isBefore(start)) return "UPCOMING";
            boolean overnight = end.isBefore(start);
            return overnight || now.isBefore(end) ? "NOT_CHECKED_IN" : "ABSENT";
        }
        if (checkOut == null) {
            boolean overnightYesterday = end.isBefore(start) && date.plusDays(1).equals(today) && now.isBefore(end.plusHours(2));
            return date.equals(today) || overnightYesterday ? "WORKING" : "MISSING_CHECK_OUT";
        }
        if (late > 0 && early > 0) return "LATE_EARLY";
        if (late > 0) return "LATE";
        if (early > 0) return "EARLY";
        return "ON_TIME";
    }

    static final String ENTRY_SELECT = """
            select s.*, u.employee_code, u.full_name, st.name as store_name,
                   r.id as record_id, r.check_in_time, r.check_out_time, coalesce(r.late_minutes, 0) as late,
                   coalesce(r.early_minutes, 0) as early,
                   exists (select 1 from leave_requests l where l.user_id = s.user_id and l.status = 'APPROVED'
                           and s.work_date between l.start_date and l.end_date) as on_leave,
                   exists (select 1 from shift_swap_requests w where (w.requester_schedule_id = s.id or w.target_schedule_id = s.id)
                           and w.status in ('PENDING_PEER', 'PENDING_APPROVAL')) as swap_pending
            from shift_schedules s
            join users u on u.id = s.user_id
            join stores st on st.id = s.store_id
            left join attendance_records r on r.user_id = s.user_id and r.attendance_date = s.work_date
            """;

    /** @param gridStore cửa hàng đang xem: ca tại cửa hàng khác chỉ hiển thị, không sửa (null = không xét) */
    static RowMapper<Entry> entryMapper(LocalDate today, LocalTime now, UUID gridStore) {
        return (ResultSet rs, int i) -> {
            LocalDate date = rs.getObject("work_date", LocalDate.class);
            LocalTime start = rs.getObject("start_time", LocalTime.class);
            LocalTime end = rs.getObject("end_time", LocalTime.class);
            int brk = rs.getInt("break_minutes");
            LocalDateTime in = DbTimes.fromDb(rs.getTimestamp("check_in_time"));
            LocalDateTime out = DbTimes.fromDb(rs.getTimestamp("check_out_time"));
            boolean recorded = rs.getObject("record_id") != null;
            UUID storeId = rs.getObject("store_id", UUID.class);
            boolean editable = !date.isBefore(today) && !recorded && (gridStore == null || gridStore.equals(storeId));
            return new Entry(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("employee_code"),
                    rs.getString("full_name"), storeId, rs.getString("store_name"), date, rs.getObject("shift_id", UUID.class),
                    rs.getString("shift_name"), rs.getString("color"), start, end, brk, ShiftTemplateService.paidMinutes(start, end, brk),
                    rs.getString("status"), rs.getString("note"), editable,
                    actualStatus(date, start, end, in, out, recorded, rs.getInt("late"), rs.getInt("early"), rs.getBoolean("on_leave"),
                            today, now),
                    in, out, rs.getInt("late"), rs.getInt("early"), rs.getBoolean("swap_pending"));
        };
    }

    private static final String AVAILABILITY_SELECT = """
            select a.user_id, a.work_date, a.kind, a.preferred_shift_id, sh.name as shift_name, a.note
            from shift_availabilities a left join shifts sh on sh.id = a.preferred_shift_id
            """;

    private static Availability mapAvailability(ResultSet rs, int i) throws SQLException {
        return new Availability(rs.getObject("user_id", UUID.class), rs.getObject("work_date", LocalDate.class), rs.getString("kind"),
                rs.getObject("preferred_shift_id", UUID.class), rs.getString("shift_name"), rs.getString("note"));
    }
}
