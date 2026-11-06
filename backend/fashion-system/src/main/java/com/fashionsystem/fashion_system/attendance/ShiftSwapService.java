package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.attendance.AttendanceAccess.Actor;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Decision;
import com.fashionsystem.fashion_system.attendance.AttendanceDtos.Rejection;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Colleague;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Entry;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.PeerResponse;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.Swap;
import com.fashionsystem.fashion_system.attendance.ShiftDtos.SwapCreate;
import com.fashionsystem.fashion_system.attendance.ShiftScheduleService.Current;
import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Đổi ca / nhường ca: nhân viên chọn ca đã công bố của mình và một đồng nghiệp (đổi lấy một ca của người đó, hoặc
 * nhờ làm thay) → đồng nghiệp đồng ý → quản lý cửa hàng duyệt thì lịch mới được cập nhật. Mọi bước đều kiểm tra lại
 * xung đột (trùng ca, nghỉ phép, đã chấm công, tháng đã chốt lương).
 */
@Service
@Transactional(readOnly = true)
public class ShiftSwapService {
    static final String PENDING_PEER = "PENDING_PEER";
    static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM");
    private static final int COLLEAGUE_DAYS = 30;

    private final NamedParameterJdbcTemplate jdbc;
    private final ShiftScheduleService schedules;
    private final RequestQueries queries;
    private final AttendanceAccess access;
    private final ShiftNotifier notifier;

    public ShiftSwapService(NamedParameterJdbcTemplate jdbc, ShiftScheduleService schedules, RequestQueries queries,
                            AttendanceAccess access, ShiftNotifier notifier) {
        this.jdbc = jdbc;
        this.schedules = schedules;
        this.queries = queries;
        this.access = access;
        this.notifier = notifier;
    }

    // ------------------------------------------------------------------ nhân viên

    /** Đồng nghiệp cùng cửa hàng của ca {@code scheduleId}: có rảnh ngày đó không, và các ca sắp tới để chọn đổi. */
    public List<Colleague> colleagues(UUID userId, UUID scheduleId) {
        Current mine = ownPublished(userId, scheduleId);
        MapSqlParameterSource p = new MapSqlParameterSource("store", mine.storeId()).addValue("user", userId)
                .addValue("date", mine.date()).addValue("from", LocalDate.now()).addValue("to", LocalDate.now().plusDays(COLLEAGUE_DAYS));
        record Row(UUID id, String code, String name, boolean free, String availability) {
        }
        List<Row> rows = jdbc.query("""
                select u.id, u.employee_code, u.full_name,
                       not exists (select 1 from shift_schedules s where s.user_id = u.id and s.work_date = :date)
                       and not exists (select 1 from attendance_records r where r.user_id = u.id and r.attendance_date = :date)
                       and not exists (select 1 from leave_requests l where l.user_id = u.id and l.status = 'APPROVED'
                                       and :date between l.start_date and l.end_date) as free,
                       (select a.kind from shift_availabilities a where a.user_id = u.id and a.work_date = :date) as availability
                from users u
                where %s and u.id <> :user
                  and exists (select 1 from store_staffs x where x.user_id = u.id and x.active and x.store_id = :store)
                order by u.full_name
                """.formatted(AttendanceAdminService.ACTIVE_EMPLOYEE), p,
                (rs, i) -> new Row(rs.getObject("id", UUID.class), rs.getString("employee_code"), rs.getString("full_name"),
                        rs.getBoolean("free"), rs.getString("availability")));
        if (rows.isEmpty()) return List.of();
        p.addValue("ids", rows.stream().map(Row::id).toList());
        Map<UUID, List<Entry>> upcoming = new HashMap<>();
        jdbc.query(ShiftScheduleService.ENTRY_SELECT + """
                 where s.store_id = :store and s.status = 'PUBLISHED' and s.user_id in (:ids) and s.work_date between :from and :to
                 order by s.work_date
                """, p, ShiftScheduleService.entryMapper(LocalDate.now(), LocalTime.now(), null))
                .stream().filter(Entry::editable).forEach(e -> upcoming.computeIfAbsent(e.userId(), k -> new java.util.ArrayList<>()).add(e));
        return rows.stream().map(r -> new Colleague(r.id(), r.code(), r.name(), r.free(), r.availability(),
                upcoming.getOrDefault(r.id(), List.of()))).toList();
    }

    public List<Swap> mine(UUID userId) {
        return jdbc.query(SELECT + " where w.requester_id = :u or w.target_user_id = :u order by w.created_at desc limit 100",
                new MapSqlParameterSource("u", userId), ShiftSwapService::map);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Swap create(UUID userId, SwapCreate request) {
        Current mine = ownPublished(userId, request.scheduleId());
        schedules.requireChangeable(mine);
        if (userId.equals(request.targetUserId())) throw BusinessException.badRequest("Không thể đổi ca với chính mình");
        requireColleague(request.targetUserId(), mine.storeId());
        Current theirs = null;
        if (request.targetScheduleId() != null) {
            theirs = schedules.current(request.targetScheduleId());
            if (!theirs.userId().equals(request.targetUserId())) throw BusinessException.badRequest("Ca muốn đổi không phải của đồng nghiệp này");
            if (!theirs.storeId().equals(mine.storeId())) throw BusinessException.badRequest("Chỉ đổi ca trong cùng cửa hàng");
            if (!ShiftScheduleService.PUBLISHED.equals(theirs.status())) throw BusinessException.badRequest("Ca của đồng nghiệp chưa được công bố");
            schedules.requireChangeable(theirs);
        }
        requireNoConflict(mine, request.targetUserId(), theirs);
        Boolean open = jdbc.queryForObject("""
                select exists (select 1 from shift_swap_requests where status in ('PENDING_PEER', 'PENDING_APPROVAL')
                               and (requester_schedule_id in (:a, :b) or target_schedule_id in (:a, :b)))
                """, new MapSqlParameterSource("a", mine.id()).addValue("b", theirs == null ? mine.id() : theirs.id()), Boolean.class);
        if (Boolean.TRUE.equals(open)) throw BusinessException.conflict("Ca này đang có yêu cầu đổi chưa xử lý xong");

        String code = queries.nextCode("DC", "shift_swap_code_seq");
        UUID id = jdbc.queryForObject("""
                insert into shift_swap_requests(code, store_id, requester_id, requester_schedule_id, target_user_id, target_schedule_id,
                                                swap_type, requester_date, requester_shift, target_date, target_shift, reason, status,
                                                created_at)
                values (:code, :store, :requester, :mine, :target, cast(:theirs as uuid), :type, :myDate, :myShift, :theirDate,
                        :theirShift, :reason, 'PENDING_PEER', now() at time zone 'UTC')
                returning id
                """, new MapSqlParameterSource("code", code).addValue("store", mine.storeId()).addValue("requester", userId)
                .addValue("mine", mine.id()).addValue("target", request.targetUserId())
                .addValue("theirs", theirs == null ? null : theirs.id(), java.sql.Types.OTHER)
                .addValue("type", theirs == null ? "GIVE_AWAY" : "SWAP").addValue("myDate", mine.date()).addValue("myShift", mine.label())
                .addValue("theirDate", theirs == null ? null : theirs.date(), java.sql.Types.DATE)
                .addValue("theirShift", theirs == null ? null : theirs.label(), java.sql.Types.VARCHAR)
                .addValue("reason", request.reason().trim()), UUID.class);
        Swap swap = get(id);
        notifier.toUsers(List.of(request.targetUserId()), userId, theirs == null ? "Nhờ làm thay ca" : "Yêu cầu đổi ca",
                swap.requesterName() + (theirs == null ? " nhờ bạn làm thay " : " muốn đổi ")
                        + describe(swap) + ". Mở Lịch làm của tôi để đồng ý hoặc từ chối.", "SHIFT_SWAP", id);
        return swap;
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Swap accept(UUID userId, UUID id, PeerResponse response) {
        Swap swap = get(id);
        if (!swap.targetUserId().equals(userId)) throw BusinessException.forbidden("Yêu cầu này không gửi cho bạn");
        requireStatus(swap, PENDING_PEER);
        revalidate(swap);
        jdbc.update("""
                update shift_swap_requests set status = 'PENDING_APPROVAL', peer_responded_at = now() at time zone 'UTC',
                       peer_note = :note, updated_at = now() at time zone 'UTC' where id = :id
                """, new MapSqlParameterSource("id", id).addValue("note", note(response)));
        swap = get(id);
        notifier.toUsers(List.of(swap.requesterId()), userId, "Đồng nghiệp đã đồng ý đổi ca",
                swap.targetName() + " đã đồng ý " + describe(swap) + ". Yêu cầu đang chờ quản lý duyệt.", "SHIFT_SWAP", id);
        notifier.toStoreManagers(swap.storeId(), userId, "Yêu cầu đổi ca chờ duyệt",
                swap.code() + ": " + swap.requesterName() + " ↔ " + swap.targetName() + " — " + describe(swap) + ".", id);
        return swap;
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Swap decline(UUID userId, UUID id, PeerResponse response) {
        Swap swap = get(id);
        if (!swap.targetUserId().equals(userId)) throw BusinessException.forbidden("Yêu cầu này không gửi cho bạn");
        requireStatus(swap, PENDING_PEER);
        jdbc.update("""
                update shift_swap_requests set status = 'DECLINED', peer_responded_at = now() at time zone 'UTC', peer_note = :note,
                       updated_at = now() at time zone 'UTC' where id = :id
                """, new MapSqlParameterSource("id", id).addValue("note", note(response)));
        swap = get(id);
        notifier.toUsers(List.of(swap.requesterId()), userId, "Đồng nghiệp từ chối đổi ca",
                swap.targetName() + " đã từ chối " + describe(swap) + ".", "SHIFT_SWAP", id);
        return swap;
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Swap cancel(UUID userId, UUID id) {
        Swap swap = get(id);
        if (!swap.requesterId().equals(userId)) throw BusinessException.forbidden("Chỉ người tạo yêu cầu được hủy");
        if (!PENDING_PEER.equals(swap.status()) && !PENDING_APPROVAL.equals(swap.status()))
            throw BusinessException.invalidState("Yêu cầu đã được xử lý, không hủy được");
        jdbc.update("update shift_swap_requests set status = 'CANCELLED', updated_at = now() at time zone 'UTC' where id = :id",
                new MapSqlParameterSource("id", id));
        notifier.toUsers(List.of(swap.targetUserId()), userId, "Yêu cầu đổi ca đã hủy",
                swap.requesterName() + " đã hủy yêu cầu " + describe(swap) + ".", "SHIFT_SWAP", id);
        return get(id);
    }

    // ------------------------------------------------------------------ quản lý

    public record Filter(String status, UUID storeId, String keyword) {
    }

    public Page<Swap> list(Actor actor, Filter filter, Pageable pageable) {
        UUID store = access.storeFilter(actor, filter.storeId());
        MapSqlParameterSource p = new MapSqlParameterSource("global", actor.global())
                .addValue("scopeStore", actor.storeId(), java.sql.Types.OTHER).addValue("store", store, java.sql.Types.OTHER)
                .addValue("status", StringUtils.hasText(filter.status()) ? filter.status().trim().toUpperCase() : null, java.sql.Types.VARCHAR)
                .addValue("kw", StringUtils.hasText(filter.keyword()) ? "%" + filter.keyword().trim().toLowerCase() + "%" : null,
                        java.sql.Types.VARCHAR);
        String where = """
                 where (:global or w.store_id = :scopeStore) and (cast(:store as uuid) is null or w.store_id = :store)
                   and (cast(:status as varchar) is null or w.status = :status)
                   and (cast(:kw as varchar) is null or lower(w.code) like :kw or lower(ru.full_name) like :kw
                        or lower(tu.full_name) like :kw or lower(coalesce(ru.employee_code, '')) like :kw)
                """;
        Integer total = jdbc.queryForObject("""
                select count(*) from shift_swap_requests w join users ru on ru.id = w.requester_id join users tu on tu.id = w.target_user_id
                """ + where, p, Integer.class);
        List<Swap> rows = jdbc.query(SELECT + where + """
                 order by (w.status = 'PENDING_APPROVAL') desc, (w.status = 'PENDING_PEER') desc, w.created_at desc
                 limit :limit offset :offset
                """, p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset()), ShiftSwapService::map);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Swap approve(Actor actor, UUID id, Decision decision) {
        Swap swap = get(id);
        requireDecider(actor, swap);
        requireStatus(swap, PENDING_APPROVAL);
        revalidate(swap);
        MapSqlParameterSource p = new MapSqlParameterSource("a", swap.requesterScheduleId())
                .addValue("b", swap.targetScheduleId(), java.sql.Types.OTHER).addValue("requester", swap.requesterId())
                .addValue("target", swap.targetUserId()).addValue("actor", actor.userId());
        if (swap.targetScheduleId() == null) {
            jdbc.update("update shift_schedules set user_id = :target, updated_by = :actor, updated_at = now() at time zone 'UTC' where id = :a", p);
        } else if (swap.requesterDate().equals(swap.targetDate())) {
            // Cùng ngày: đổi nội dung ca giữa hai dòng (đổi user_id sẽ vướng ràng buộc một ca / người / ngày giữa chừng).
            jdbc.update("""
                    update shift_schedules x set shift_id = y.shift_id, shift_name = y.shift_name, color = y.color,
                           start_time = y.start_time, end_time = y.end_time, break_minutes = y.break_minutes, note = y.note,
                           updated_by = :actor, updated_at = now() at time zone 'UTC'
                    from shift_schedules y
                    where (x.id = :a and y.id = cast(:b as uuid)) or (x.id = cast(:b as uuid) and y.id = :a)
                    """, p);
        } else {
            jdbc.update("update shift_schedules set user_id = :target, updated_by = :actor, updated_at = now() at time zone 'UTC' where id = :a", p);
            jdbc.update("update shift_schedules set user_id = :requester, updated_by = :actor, updated_at = now() at time zone 'UTC' where id = cast(:b as uuid)", p);
        }
        jdbc.update("""
                update shift_swap_requests set status = 'APPROVED', decided_by = :actor, decided_at = now() at time zone 'UTC',
                       approver_note = :note, updated_at = now() at time zone 'UTC' where id = :id
                """, new MapSqlParameterSource("id", id).addValue("actor", actor.userId())
                .addValue("note", decision == null ? null : RequestQueries.trim(decision.note()), java.sql.Types.VARCHAR));
        // Các yêu cầu khác đang mở trên hai ca này không còn đúng nữa.
        jdbc.update("""
                update shift_swap_requests set status = 'CANCELLED', rejection_reason = 'Ca đã được đổi theo yêu cầu khác',
                       updated_at = now() at time zone 'UTC'
                where id <> :id and status in ('PENDING_PEER', 'PENDING_APPROVAL')
                  and (requester_schedule_id in (:a, coalesce(cast(:b as uuid), :a)) or target_schedule_id in (:a, coalesce(cast(:b as uuid), :a)))
                """, p.addValue("id", id));
        swap = get(id);
        notifier.toUsers(List.of(swap.requesterId(), swap.targetUserId()), actor.userId(), "Đổi ca đã được duyệt",
                "Yêu cầu " + swap.code() + " (" + describe(swap) + ") đã được duyệt, lịch làm đã cập nhật.", "SHIFT_SWAP", id);
        return swap;
    }

    @Transactional
    @BusinessAudit("ATTENDANCE")
    public Swap reject(Actor actor, UUID id, Rejection rejection) {
        Swap swap = get(id);
        requireDecider(actor, swap);
        if (!PENDING_PEER.equals(swap.status()) && !PENDING_APPROVAL.equals(swap.status()))
            throw BusinessException.invalidState("Yêu cầu đã được xử lý");
        jdbc.update("""
                update shift_swap_requests set status = 'REJECTED', decided_by = :actor, decided_at = now() at time zone 'UTC',
                       rejection_reason = :reason, updated_at = now() at time zone 'UTC' where id = :id
                """, new MapSqlParameterSource("id", id).addValue("actor", actor.userId()).addValue("reason", rejection.reason().trim()));
        swap = get(id);
        notifier.toUsers(List.of(swap.requesterId(), swap.targetUserId()), actor.userId(), "Đổi ca bị từ chối",
                "Yêu cầu " + swap.code() + " (" + describe(swap) + ") bị từ chối: " + rejection.reason().trim(), "SHIFT_SWAP", id);
        return swap;
    }

    // ------------------------------------------------------------------ kiểm tra

    private Current ownPublished(UUID userId, UUID scheduleId) {
        Current mine = schedules.current(scheduleId);
        if (!mine.userId().equals(userId)) throw BusinessException.forbidden("Đây không phải ca của bạn");
        if (!ShiftScheduleService.PUBLISHED.equals(mine.status())) throw BusinessException.badRequest("Chỉ đổi được ca đã công bố");
        return mine;
    }

    private void requireColleague(UUID userId, UUID storeId) {
        Boolean ok = jdbc.queryForObject("""
                select exists (select 1 from users u where u.id = :u and %s
                               and exists (select 1 from store_staffs x where x.user_id = u.id and x.active and x.store_id = :s))
                """.formatted(AttendanceAdminService.ACTIVE_EMPLOYEE), new MapSqlParameterSource("u", userId).addValue("s", storeId),
                Boolean.class);
        if (!Boolean.TRUE.equals(ok)) throw BusinessException.badRequest("Chỉ đổi ca với đồng nghiệp đang làm cùng cửa hàng");
    }

    /**
     * Sau khi đổi không ai bị hai ca một ngày hoặc phải làm ngày đang nghỉ phép: nhường ca → người nhận phải trống ngày
     * đó; đổi khác ngày → mỗi người phải trống ngày của ca mới. Đổi cùng ngày thì mỗi người vẫn một ca.
     */
    private void requireNoConflict(Current mine, UUID targetUserId, Current theirs) {
        if (theirs == null) {
            schedules.requireSchedulable(targetUserId, mine.date(), null);
            return;
        }
        if (mine.date().equals(theirs.date())) return;
        schedules.requireSchedulable(targetUserId, mine.date(), null);
        schedules.requireSchedulable(mine.userId(), theirs.date(), null);
    }

    /** Lịch có thể đã đổi từ lúc tạo đơn: kiểm tra lại ca còn nguyên chủ, còn sửa được và không xung đột. */
    private void revalidate(Swap swap) {
        if (swap.requesterScheduleId() == null || (swap.swapType().equals("SWAP") && swap.targetScheduleId() == null))
            throw BusinessException.conflict("Ca trong yêu cầu đã bị xóa khỏi lịch");
        Current mine = schedules.current(swap.requesterScheduleId());
        if (!mine.userId().equals(swap.requesterId())) throw BusinessException.conflict("Ca của người yêu cầu đã thay đổi");
        schedules.requireChangeable(mine);
        Current theirs = null;
        if (swap.targetScheduleId() != null) {
            theirs = schedules.current(swap.targetScheduleId());
            if (!theirs.userId().equals(swap.targetUserId())) throw BusinessException.conflict("Ca của đồng nghiệp đã thay đổi");
            schedules.requireChangeable(theirs);
        }
        requireNoConflict(mine, swap.targetUserId(), theirs);
    }

    private void requireDecider(Actor actor, Swap swap) {
        schedules.requireStore(actor, swap.storeId());
        if (actor.userId().equals(swap.requesterId()) || actor.userId().equals(swap.targetUserId()))
            throw BusinessException.forbidden("Không thể tự duyệt yêu cầu đổi ca của chính mình");
    }

    private static void requireStatus(Swap swap, String status) {
        if (!status.equals(swap.status())) throw BusinessException.invalidState("Yêu cầu không còn ở bước này");
    }

    private static String note(PeerResponse response) {
        return response == null ? null : RequestQueries.trim(response.note());
    }

    private static String describe(Swap s) {
        String mine = "ca " + s.requesterShift() + " ngày " + s.requesterDate().format(DM);
        return s.targetScheduleId() == null && s.targetDate() == null ? mine
                : mine + " lấy ca " + s.targetShift() + " ngày " + s.targetDate().format(DM);
    }

    Swap get(UUID id) {
        List<Swap> rows = jdbc.query(SELECT + " where w.id = :id", new MapSqlParameterSource("id", id), ShiftSwapService::map);
        if (rows.isEmpty()) throw BusinessException.notFound("Yêu cầu đổi ca không tồn tại");
        return rows.getFirst();
    }

    private static final String SELECT = """
            select w.*, st.name as store_name, ru.employee_code as r_code, ru.full_name as r_name, tu.employee_code as t_code,
                   tu.full_name as t_name, d.full_name as decided_name
            from shift_swap_requests w
            join stores st on st.id = w.store_id
            join users ru on ru.id = w.requester_id
            join users tu on tu.id = w.target_user_id
            left join users d on d.id = w.decided_by
            """;

    private static Swap map(ResultSet rs, int i) throws SQLException {
        return new Swap(rs.getObject("id", UUID.class), rs.getString("code"), rs.getObject("store_id", UUID.class),
                rs.getString("store_name"), rs.getString("swap_type"), rs.getObject("requester_id", UUID.class), rs.getString("r_code"),
                rs.getString("r_name"), rs.getObject("requester_schedule_id", UUID.class), rs.getObject("requester_date", LocalDate.class),
                rs.getString("requester_shift"), rs.getObject("target_user_id", UUID.class), rs.getString("t_code"), rs.getString("t_name"),
                rs.getObject("target_schedule_id", UUID.class), rs.getObject("target_date", LocalDate.class), rs.getString("target_shift"),
                rs.getString("reason"), rs.getString("status"), rs.getString("peer_note"), DbTimes.fromDb(rs.getTimestamp("peer_responded_at")),
                rs.getString("decided_name"), DbTimes.fromDb(rs.getTimestamp("decided_at")), rs.getString("rejection_reason"),
                rs.getString("approver_note"), DbTimes.fromDb(rs.getTimestamp("created_at")));
    }
}
