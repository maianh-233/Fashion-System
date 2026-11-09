package com.fashionsystem.fashion_system.task;

import com.fashionsystem.fashion_system.audit.BusinessAudit;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Actions;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.ChecklistInput;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.ChecklistItem;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Person;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.StatRow;
import com.fashionsystem.fashion_system.task.WorkTaskDtos.Task;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Giao việc và theo dõi công việc. Trạng thái: Cần làm → Đang làm → Hoàn thành (người được giao xác nhận, người giao được báo);
 * người giao có thể trả lại về Đang làm hoặc hủy. Mỗi người nhận có một dòng riêng, các dòng cùng đợt giao chung batch_id.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkTaskService {
    static final String TODO = "TODO";
    static final String IN_PROGRESS = "IN_PROGRESS";
    static final String DONE = "DONE";
    static final String CANCELLED = "CANCELLED";
    private static final String OPEN_STATUSES = "('TODO', 'IN_PROGRESS')";
    private static final String NOW = "(now() at time zone 'UTC')";
    private static final Set<String> PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> STATUSES = Set.of(TODO, IN_PROGRESS, DONE, CANCELLED);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter DUE_TEXT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final String PRIORITY_RANK =
            "case t.priority when 'URGENT' then 0 when 'HIGH' then 1 when 'NORMAL' then 2 else 3 end";
    private static final String OPEN_ORDER = " order by (t.status in " + OPEN_STATUSES + ") desc, t.due_at asc nulls last, "
            + PRIORITY_RANK + ", t.created_at desc";

    private static final String SELECT = """
            select t.*, coalesce(ar.full_name, ar.username) as assigner_name, coalesce(ae.full_name, ae.username) as assignee_name,
                   s.name as store_name,
                   (select count(*) from work_task_checklist_items c where c.task_id = t.id) as checklist_total,
                   (select count(*) from work_task_checklist_items c where c.task_id = t.id and c.done) as checklist_done,
                   (select count(*) from work_task_comments c where c.task_id = t.id) as comment_count
            """;
    private static final String FROM = """
            from work_tasks t
            join users ae on ae.id = t.assignee_id
            left join users ar on ar.id = t.assigner_id
            left join stores s on s.id = t.store_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final WorkTaskAccess access;
    private final WorkTaskNotifier notifier;

    // ------------------------------------------------------------------ danh mục

    public WorkTaskDtos.Lookups lookups(UUID actor) {
        UUID store = access.ownScope(actor);
        boolean global = store == null;
        boolean canView = access.canViewScope(actor, store);
        List<Person> assignees = access.assignable(actor, null);
        List<Person> people = canView ? access.peopleInScope(store) : List.of();
        List<WorkTaskDtos.Ref> stores = canView && global
                ? jdbc.query("select id, name from stores where active order by name", new MapSqlParameterSource(),
                        (rs, i) -> new WorkTaskDtos.Ref(rs.getObject("id", UUID.class), rs.getString("name")))
                : List.of();
        return new WorkTaskDtos.Lookups(!assignees.isEmpty(), canView, global, store, stores, assignees, people);
    }

    // ------------------------------------------------------------------ giao và sửa việc

    @Transactional
    @BusinessAudit("WORK_TASK")
    public List<Task> create(UUID actor, WorkTaskDtos.CreateRequest request) {
        Set<UUID> ids = new LinkedHashSet<>(request.assigneeIds());
        List<Person> assignees = access.assignable(actor, ids);
        if (assignees.size() != ids.size()) throw BusinessException.forbidden("Có nhân viên nằm ngoài phạm vi bạn được giao việc");
        requireFuture(request.dueAt());
        String priority = priority(request.priority());
        UUID batch = UUID.randomUUID();
        String actorName = name(actor);
        List<Task> created = new ArrayList<>();
        for (Person assignee : assignees) {
            UUID id = jdbc.queryForObject("""
                    insert into work_tasks(code, batch_id, title, description, priority, due_at, assigner_id, assignee_id, store_id)
                    values (:code, :batch, :title, :description, :priority, :due, :assigner, :assignee, cast(:store as uuid))
                    returning id
                    """, new MapSqlParameterSource("code", nextCode()).addValue("batch", batch)
                    .addValue("title", request.title().trim())
                    .addValue("description", trim(request.description()), Types.VARCHAR).addValue("priority", priority)
                    .addValue("due", DbTimes.toDb(request.dueAt()), Types.TIMESTAMP).addValue("assigner", actor)
                    .addValue("assignee", assignee.id()).addValue("store", assignee.storeId(), Types.OTHER), UUID.class);
            insertChecklist(id, request.checklist(), 0);
            history(id, "CREATED", null, TODO, null, actor);
            Task task = task(id);
            created.add(task);
            notifier.send(List.of(assignee.id()), actor, NotificationType.TASK_ASSIGNED, "Bạn có việc mới: " + task.title(),
                    actorName + " giao cho bạn" + dueSuffix(task.dueAt()) + ".", task);
        }
        return created;
    }

    @Transactional
    @BusinessAudit("WORK_TASK")
    public WorkTaskDtos.Detail update(UUID actor, UUID id, WorkTaskDtos.UpdateRequest request) {
        Task task = lock(id);
        if (!actor.equals(task.assignerId())) throw BusinessException.forbidden("Chỉ người giao việc mới sửa được công việc này");
        requireOpen(task);
        boolean dueChanged = !Objects.equals(task.dueAt(), request.dueAt());
        if (dueChanged) requireFuture(request.dueAt());
        String title = request.title().trim();
        String description = trim(request.description());
        String priority = priority(request.priority());
        boolean checklistChanged = syncChecklist(id, request.checklist());
        if (!dueChanged && !checklistChanged && title.equals(task.title()) && Objects.equals(description, task.description())
                && priority.equals(task.priority())) {
            return detail(actor, id);
        }
        jdbc.update("update work_tasks set title = :title, description = :description, priority = :priority, due_at = :due,"
                        + " due_soon_notified_at = case when :reset then null else due_soon_notified_at end,"
                        + " overdue_notified_at = case when :reset then null else overdue_notified_at end,"
                        + " updated_at = " + NOW + " where id = :id",
                new MapSqlParameterSource("id", id).addValue("title", title).addValue("description", description, Types.VARCHAR)
                        .addValue("priority", priority).addValue("due", DbTimes.toDb(request.dueAt()), Types.TIMESTAMP)
                        .addValue("reset", dueChanged));
        history(id, "UPDATED", task.status(), task.status(), null, actor);
        Task saved = task(id);
        notifier.send(List.of(saved.assigneeId()), actor, NotificationType.TASK_UPDATED, "Công việc được cập nhật: " + saved.title(),
                name(actor) + " đã sửa nội dung công việc" + dueSuffix(saved.dueAt()) + ".", saved);
        return detail(actor, id);
    }

    // ------------------------------------------------------------------ chuyển trạng thái

    @Transactional
    @BusinessAudit("WORK_TASK")
    public WorkTaskDtos.Detail start(UUID actor, UUID id) {
        Task task = lock(id);
        requireAssignee(actor, task);
        if (!TODO.equals(task.status())) throw BusinessException.conflict("Chỉ bắt đầu được công việc đang ở trạng thái Cần làm");
        markStarted(actor, id);
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("WORK_TASK")
    public WorkTaskDtos.Detail complete(UUID actor, UUID id, WorkTaskDtos.CompleteRequest request) {
        Task task = lock(id);
        requireAssignee(actor, task);
        requireOpen(task);
        if (task.checklistDone() < task.checklistTotal()) {
            throw BusinessException.conflict("Còn " + (task.checklistTotal() - task.checklistDone()) + " việc con chưa hoàn thành");
        }
        String note = request == null ? null : trim(request.note());
        jdbc.update("update work_tasks set status = 'DONE', completion_note = :note, completed_at = " + NOW + ", return_reason = null,"
                + " updated_at = " + NOW + " where id = :id", new MapSqlParameterSource("id", id).addValue("note", note, Types.VARCHAR));
        history(id, "COMPLETED", task.status(), DONE, note, actor);
        Task saved = task(id);
        if (saved.assignerId() != null) {
            notifier.send(List.of(saved.assignerId()), actor, NotificationType.TASK_COMPLETED,
                    saved.assigneeName() + " đã hoàn thành: " + saved.title(),
                    note != null ? note : (saved.late() ? "Hoàn thành trễ hạn." : "Công việc đã được xác nhận hoàn thành."), saved);
        }
        return detail(actor, id);
    }

    /** Việc chưa đạt quay về Đang làm; các việc con đã tick được giữ nguyên. */
    @Transactional
    @BusinessAudit("WORK_TASK")
    public WorkTaskDtos.Detail giveBack(UUID actor, UUID id, WorkTaskDtos.ReturnRequest request) {
        Task task = lock(id);
        if (!DONE.equals(task.status())) throw BusinessException.conflict("Chỉ trả lại được công việc đã hoàn thành");
        if (!access.actions(actor, task).giveBack()) throw BusinessException.forbidden("Bạn không có quyền trả lại công việc này");
        String reason = request.reason().trim();
        jdbc.update("update work_tasks set status = 'IN_PROGRESS', completed_at = null, completion_note = null, return_reason = :reason,"
                + " return_count = return_count + 1, updated_at = " + NOW + " where id = :id",
                new MapSqlParameterSource("id", id).addValue("reason", reason));
        history(id, "RETURNED", DONE, IN_PROGRESS, reason, actor);
        Task saved = task(id);
        notifier.send(List.of(saved.assigneeId()), actor, NotificationType.TASK_RETURNED, "Công việc bị trả lại: " + saved.title(),
                "Lý do: " + reason, saved);
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("WORK_TASK")
    public WorkTaskDtos.Detail cancel(UUID actor, UUID id, WorkTaskDtos.CancelRequest request) {
        Task task = lock(id);
        requireOpen(task);
        if (!access.actions(actor, task).cancel()) throw BusinessException.forbidden("Bạn không có quyền hủy công việc này");
        String reason = request == null ? null : trim(request.reason());
        jdbc.update("update work_tasks set status = 'CANCELLED', updated_at = " + NOW + " where id = :id", new MapSqlParameterSource("id", id));
        history(id, "CANCELLED", task.status(), CANCELLED, reason, actor);
        Task saved = task(id);
        notifier.send(List.of(saved.assigneeId()), actor, NotificationType.TASK_CANCELLED, "Công việc đã bị hủy: " + saved.title(),
                reason != null ? "Lý do: " + reason : name(actor) + " đã hủy công việc này.", saved);
        return detail(actor, id);
    }

    /** Tick / bỏ tick việc con; tick đầu tiên tự chuyển việc sang Đang làm. */
    @Transactional
    @BusinessAudit("WORK_TASK")
    public WorkTaskDtos.Detail toggleItem(UUID actor, UUID id, UUID itemId, boolean done) {
        Task task = lock(id);
        requireAssignee(actor, task);
        requireOpen(task);
        int updated = jdbc.update("update work_task_checklist_items set done = :done, done_at = case when :done then " + NOW
                        + " else null end where id = :item and task_id = :task",
                new MapSqlParameterSource("done", done).addValue("item", itemId).addValue("task", id));
        if (updated == 0) throw BusinessException.notFound("Việc con không tồn tại");
        if (done && TODO.equals(task.status())) markStarted(actor, id);
        return detail(actor, id);
    }

    @Transactional
    @BusinessAudit("WORK_TASK")
    public WorkTaskDtos.Detail addComment(UUID actor, UUID id, WorkTaskDtos.CommentRequest request) {
        Task task = task(id);
        if (!access.actions(actor, task).comment()) throw BusinessException.forbidden("Bạn không thể bình luận trong công việc này");
        String content = request.content().trim();
        jdbc.update("insert into work_task_comments(task_id, author_id, content) values (:task, :author, :content)",
                new MapSqlParameterSource("task", id).addValue("author", actor).addValue("content", content));
        Set<UUID> to = new HashSet<>();
        to.add(task.assigneeId());
        if (task.assignerId() != null) to.add(task.assignerId());
        notifier.send(to, actor, NotificationType.TASK_COMMENTED, name(actor) + " bình luận: " + task.title(), content, task);
        return detail(actor, id);
    }

    // ------------------------------------------------------------------ truy vấn

    public WorkTaskDtos.Detail detail(UUID actor, UUID id) {
        Task task = task(id);
        Actions can = access.actions(actor, task);
        if (!can.view()) throw BusinessException.forbidden("Bạn không có quyền xem công việc này");
        MapSqlParameterSource p = new MapSqlParameterSource("id", id);
        List<ChecklistItem> checklist = jdbc.query("select * from work_task_checklist_items where task_id = :id order by sort_order, id", p,
                (rs, i) -> new ChecklistItem(rs.getObject("id", UUID.class), rs.getString("content"), rs.getBoolean("done"),
                        DbTimes.fromDb(rs.getObject("done_at"))));
        List<WorkTaskDtos.Comment> comments = jdbc.query("""
                select c.*, coalesce(u.full_name, u.username) as author_name from work_task_comments c
                left join users u on u.id = c.author_id where c.task_id = :id order by c.created_at, c.id
                """, p, (rs, i) -> new WorkTaskDtos.Comment(rs.getObject("id", UUID.class), rs.getObject("author_id", UUID.class),
                rs.getString("author_name"), rs.getString("content"), DbTimes.fromDb(rs.getObject("created_at"))));
        List<WorkTaskDtos.History> history = jdbc.query("""
                select h.*, coalesce(u.full_name, u.username) as actor_name from work_task_history h
                left join users u on u.id = h.actor_id where h.task_id = :id order by h.created_at desc, h.id
                """, p, (rs, i) -> new WorkTaskDtos.History(rs.getObject("id", UUID.class), rs.getString("action"),
                rs.getString("from_status"), rs.getString("to_status"), rs.getString("note"), rs.getString("actor_name"),
                DbTimes.fromDb(rs.getObject("created_at"))));
        List<Task> batch = can.viewBatch()
                ? jdbc.query(SELECT + FROM + " where t.batch_id = :batch order by assignee_name",
                        new MapSqlParameterSource("batch", task.batchId()), this::mapTask)
                : List.of();
        return new WorkTaskDtos.Detail(task, checklist, comments, history, batch, can);
    }

    /** Toàn bộ công việc trong phạm vi: nhân viên cửa hàng chỉ thấy việc của cửa hàng mình. */
    public Page<Task> list(UUID actor, UUID storeId, String status, Boolean overdue, UUID assigneeId, UUID assignerId, String priority,
                           String keyword, Pageable pageable) {
        UUID store = access.requireListScope(actor, storeId);
        String where = " where (cast(:store as uuid) is null or t.store_id = :store)"
                + " and (cast(:assignee as uuid) is null or t.assignee_id = :assignee)"
                + " and (cast(:assigner as uuid) is null or t.assigner_id = :assigner)"
                + " and (cast(:keyword as varchar) is null or t.title ilike :keyword or t.code ilike :keyword"
                + " or ae.full_name ilike :keyword)"
                + filters(status, overdue, priority);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("store", store, Types.OTHER)
                .addValue("assignee", assigneeId, Types.OTHER).addValue("assigner", assignerId, Types.OTHER)
                .addValue("keyword", StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null, Types.VARCHAR);
        bindFilters(p, status, priority);
        return page(where, OPEN_ORDER, p, pageable);
    }

    /**
     * Việc của tôi.
     *
     * @param view  ASSIGNED_TO_ME (mặc định) hoặc ASSIGNED_BY_ME
     * @param state OPEN (Cần làm + Đang làm), CLOSED (Hoàn thành + Đã hủy) hoặc null là tất cả
     */
    public Page<Task> mine(UUID actor, String view, String state, String priority, Pageable pageable) {
        boolean byMe = "ASSIGNED_BY_ME".equalsIgnoreCase(view);
        boolean closed = "CLOSED".equalsIgnoreCase(state);
        String where = " where " + (byMe ? "t.assigner_id" : "t.assignee_id") + " = :actor"
                + ("OPEN".equalsIgnoreCase(state) ? " and t.status in " + OPEN_STATUSES : closed ? " and t.status in ('DONE', 'CANCELLED')" : "")
                + filters(null, null, priority);
        MapSqlParameterSource p = new MapSqlParameterSource("actor", actor);
        bindFilters(p, null, priority);
        return page(where, closed ? " order by coalesce(t.completed_at, t.updated_at, t.created_at) desc" : OPEN_ORDER, p, pageable);
    }

    /** Thống kê theo người nhận, tính các việc được giao trong khoảng ngày (không gồm việc đã hủy). */
    public WorkTaskDtos.Stats stats(UUID actor, UUID storeId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw BusinessException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        UUID store = access.requireListScope(actor, storeId);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("store", store, Types.OTHER)
                .addValue("from", DbTimes.toDb(from.atStartOfDay()), Types.TIMESTAMP)
                .addValue("to", DbTimes.toDb(to.plusDays(1).atStartOfDay()), Types.TIMESTAMP);
        String metrics = """
                count(*) as total,
                count(*) filter (where t.status in ('TODO', 'IN_PROGRESS')) as open_count,
                count(*) filter (where t.status = 'DONE') as done_count,
                count(*) filter (where t.status in ('TODO', 'IN_PROGRESS') and t.due_at < (now() at time zone 'UTC')) as overdue_count,
                count(*) filter (where t.status = 'DONE' and t.completed_at <= t.due_at) as on_time_count,
                count(*) filter (where t.status = 'DONE' and t.completed_at > t.due_at) as late_count
                """;
        String where = """
                from work_tasks t
                join users ae on ae.id = t.assignee_id
                left join stores s on s.id = t.store_id
                where t.status <> 'CANCELLED' and t.created_at >= :from and t.created_at < :to
                  and (cast(:store as uuid) is null or t.store_id = :store)
                """;
        List<StatRow> rows = jdbc.query("select t.assignee_id, coalesce(ae.full_name, ae.username) as assignee_name, max(s.name) as store_name, "
                        + metrics + where + " group by t.assignee_id, 2 order by overdue_count desc, total desc, 2",
                p, (rs, i) -> statRow(rs, rs.getObject("assignee_id", UUID.class), rs.getString("assignee_name"), rs.getString("store_name")));
        StatRow totals = jdbc.queryForObject("select " + metrics + where, p, (rs, i) -> statRow(rs, null, null, null));
        return new WorkTaskDtos.Stats(totals, rows);
    }

    // ------------------------------------------------------------------ nhắc hạn

    /** Gửi nhắc sắp đến hạn và báo quá hạn; UPDATE ... RETURNING đánh dấu nguyên tử nên mỗi mốc chỉ gửi một lần. */
    @Transactional
    public int sendDueReminders() {
        List<Reminder> dueSoon = jdbc.query("update work_tasks set due_soon_notified_at = " + NOW
                + " where status in " + OPEN_STATUSES + " and due_soon_notified_at is null and due_at > " + NOW
                + " and due_at <= " + NOW + " + interval '24 hours' returning id", new MapSqlParameterSource(),
                (rs, i) -> new Reminder(rs.getObject("id", UUID.class)));
        List<Reminder> overdue = jdbc.query("update work_tasks set overdue_notified_at = " + NOW
                + " where status in " + OPEN_STATUSES + " and overdue_notified_at is null and due_at <= " + NOW + " returning id",
                new MapSqlParameterSource(), (rs, i) -> new Reminder(rs.getObject("id", UUID.class)));
        for (Reminder reminder : dueSoon) {
            Task task = task(reminder.id());
            notifier.send(List.of(task.assigneeId()), null, NotificationType.TASK_DUE_SOON, "Sắp đến hạn: " + task.title(),
                    "Hạn chót lúc " + DUE_TEXT.format(task.dueAt()) + ".", task);
        }
        for (Reminder reminder : overdue) {
            Task task = task(reminder.id());
            Set<UUID> to = new HashSet<>();
            to.add(task.assigneeId());
            if (task.assignerId() != null) to.add(task.assignerId());
            notifier.send(to, null, NotificationType.TASK_OVERDUE, "Quá hạn: " + task.title(),
                    task.assigneeName() + " chưa hoàn thành, hạn chót là " + DUE_TEXT.format(task.dueAt()) + ".", task);
        }
        return dueSoon.size() + overdue.size();
    }

    private record Reminder(UUID id) {}

    // ------------------------------------------------------------------ hỗ trợ

    private Page<Task> page(String where, String order, MapSqlParameterSource p, Pageable pageable) {
        Long total = jdbc.queryForObject("select count(*) " + FROM + where, p, Long.class);
        p.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<Task> rows = jdbc.query(SELECT + FROM + where + order + " limit :limit offset :offset", p, this::mapTask);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    private static String filters(String status, Boolean overdue, String priority) {
        return (StringUtils.hasText(status) ? " and t.status = :status" : "")
                + (StringUtils.hasText(priority) ? " and t.priority = :priority" : "")
                + (Boolean.TRUE.equals(overdue) ? " and t.status in " + OPEN_STATUSES + " and t.due_at < " + NOW : "");
    }

    private static void bindFilters(MapSqlParameterSource p, String status, String priority) {
        if (StringUtils.hasText(status)) {
            String value = status.trim().toUpperCase();
            if (!STATUSES.contains(value)) throw BusinessException.badRequest("Trạng thái không hợp lệ");
            p.addValue("status", value);
        }
        if (StringUtils.hasText(priority)) p.addValue("priority", priority(priority));
    }

    private Task task(UUID id) {
        try {
            return jdbc.queryForObject(SELECT + FROM + " where t.id = :id", new MapSqlParameterSource("id", id), this::mapTask);
        } catch (EmptyResultDataAccessException missing) {
            throw BusinessException.notFound("Công việc không tồn tại");
        }
    }

    private Task lock(UUID id) {
        List<UUID> locked = jdbc.queryForList("select id from work_tasks where id = :id for update", new MapSqlParameterSource("id", id), UUID.class);
        if (locked.isEmpty()) throw BusinessException.notFound("Công việc không tồn tại");
        return task(id);
    }

    private Task mapTask(ResultSet rs, int row) throws SQLException {
        String status = rs.getString("status");
        LocalDateTime due = DbTimes.fromDb(rs.getObject("due_at"));
        LocalDateTime completed = DbTimes.fromDb(rs.getObject("completed_at"));
        boolean open = TODO.equals(status) || IN_PROGRESS.equals(status);
        return new Task(rs.getObject("id", UUID.class), rs.getString("code"), rs.getObject("batch_id", UUID.class), rs.getString("title"),
                rs.getString("description"), rs.getString("priority"), status, due,
                open && due != null && due.isBefore(LocalDateTime.now()),
                DONE.equals(status) && due != null && completed != null && completed.isAfter(due),
                rs.getObject("assigner_id", UUID.class), rs.getString("assigner_name"), rs.getObject("assignee_id", UUID.class),
                rs.getString("assignee_name"), rs.getObject("store_id", UUID.class), rs.getString("store_name"),
                rs.getInt("checklist_total"), rs.getInt("checklist_done"), rs.getInt("comment_count"), rs.getString("completion_note"),
                completed, rs.getString("return_reason"), rs.getInt("return_count"), DbTimes.fromDb(rs.getObject("created_at")),
                DbTimes.fromDb(rs.getObject("updated_at")));
    }

    private static StatRow statRow(ResultSet rs, UUID assignee, String name, String store) throws SQLException {
        int onTime = rs.getInt("on_time_count");
        int late = rs.getInt("late_count");
        Integer rate = onTime + late == 0 ? null : Math.round(onTime * 100f / (onTime + late));
        return new StatRow(assignee, name, store, rs.getInt("total"), rs.getInt("open_count"), rs.getInt("done_count"),
                rs.getInt("overdue_count"), onTime, late, rate);
    }

    private void markStarted(UUID actor, UUID id) {
        jdbc.update("update work_tasks set status = 'IN_PROGRESS', updated_at = " + NOW + " where id = :id", new MapSqlParameterSource("id", id));
        history(id, "STARTED", TODO, IN_PROGRESS, null, actor);
    }

    private void history(UUID task, String action, String from, String to, String note, UUID actor) {
        jdbc.update("insert into work_task_history(task_id, action, from_status, to_status, note, actor_id)"
                        + " values (:task, :action, :from, :to, :note, cast(:actor as uuid))",
                new MapSqlParameterSource("task", task).addValue("action", action).addValue("from", from, Types.VARCHAR)
                        .addValue("to", to, Types.VARCHAR).addValue("note", note, Types.VARCHAR).addValue("actor", actor, Types.OTHER));
    }

    private void insertChecklist(UUID task, List<ChecklistInput> items, int startOrder) {
        if (items == null) return;
        int order = startOrder;
        for (ChecklistInput item : items) {
            jdbc.update("insert into work_task_checklist_items(task_id, content, sort_order) values (:task, :content, :order)",
                    new MapSqlParameterSource("task", task).addValue("content", item.content().trim()).addValue("order", order++));
        }
    }

    /** Đồng bộ checklist theo danh sách gửi lên: giữ trạng thái tick của dòng cũ, xóa dòng bị bỏ. Trả về true nếu có thay đổi. */
    private boolean syncChecklist(UUID task, List<ChecklistInput> items) {
        List<ChecklistInput> wanted = items == null ? List.of() : items;
        MapSqlParameterSource p = new MapSqlParameterSource("task", task);
        List<ChecklistInput> current = jdbc.query("select id, content from work_task_checklist_items where task_id = :task order by sort_order, id", p,
                (rs, i) -> new ChecklistInput(rs.getObject("id", UUID.class), rs.getString("content")));
        Set<UUID> known = new HashSet<>(current.stream().map(ChecklistInput::id).toList());
        List<ChecklistInput> normalized = wanted.stream().map(i -> new ChecklistInput(i.id(), i.content().trim())).toList();
        if (normalized.equals(current)) return false;
        Set<UUID> kept = new HashSet<>();
        for (ChecklistInput item : normalized) if (item.id() != null) kept.add(item.id());
        if (!known.containsAll(kept)) throw BusinessException.badRequest("Việc con không thuộc công việc này");
        for (UUID removed : known) {
            if (!kept.contains(removed)) {
                jdbc.update("delete from work_task_checklist_items where id = :id", new MapSqlParameterSource("id", removed));
            }
        }
        int order = 0;
        for (ChecklistInput item : normalized) {
            MapSqlParameterSource row = new MapSqlParameterSource("task", task).addValue("content", item.content()).addValue("order", order++);
            if (item.id() == null) {
                jdbc.update("insert into work_task_checklist_items(task_id, content, sort_order) values (:task, :content, :order)", row);
            } else {
                jdbc.update("update work_task_checklist_items set content = :content, sort_order = :order where id = :id and task_id = :task",
                        row.addValue("id", item.id()));
            }
        }
        return true;
    }

    private static void requireAssignee(UUID actor, Task task) {
        if (!actor.equals(task.assigneeId())) throw BusinessException.forbidden("Chỉ người được giao mới cập nhật được tiến độ công việc này");
    }

    private static void requireOpen(Task task) {
        if (!TODO.equals(task.status()) && !IN_PROGRESS.equals(task.status())) {
            throw BusinessException.conflict("Công việc đã kết thúc, không thể cập nhật");
        }
    }

    private static void requireFuture(LocalDateTime due) {
        if (due != null && !due.isAfter(LocalDateTime.now())) throw BusinessException.badRequest("Hạn chót phải sau thời điểm hiện tại");
    }

    private static String priority(String value) {
        String priority = StringUtils.hasText(value) ? value.trim().toUpperCase() : "NORMAL";
        if (!PRIORITIES.contains(priority)) throw BusinessException.badRequest("Mức ưu tiên không hợp lệ");
        return priority;
    }

    private String nextCode() {
        Long value = jdbc.getJdbcTemplate().queryForObject("select nextval('work_task_code_seq')", Long.class);
        return "CV-%s-%05d".formatted(LocalDate.now(ZONE).format(CODE_DATE), value);
    }

    private String name(UUID user) {
        Map<String, Object> row = jdbc.queryForMap("select coalesce(full_name, username) as name from users where id = :id",
                new MapSqlParameterSource("id", user));
        return (String) row.get("name");
    }

    private static String dueSuffix(LocalDateTime due) {
        return due == null ? "" : ", hạn chót " + DUE_TEXT.format(due);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
