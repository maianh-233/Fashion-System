package com.fashionsystem.fashion_system.recruitment;

import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.time;
import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.trim;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.AppState;
import com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.StageRow;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Interview;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.InterviewRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.UserRef;
import com.fashionsystem.fashion_system.service.RbacService;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Lịch phỏng vấn: tạo lịch, phân công người phỏng vấn (tự thành người đánh giá của vòng), dời lịch, ghi kết quả.
 * Hoàn thành phỏng vấn → vòng "Chờ đánh giá", hồ sơ "Chờ đánh giá".
 */
@Service
@Transactional(readOnly = true)
public class InterviewService {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    static final String[] MODES = {"ONSITE", "ONLINE", "PHONE"};

    private final NamedParameterJdbcTemplate jdbc;
    private final RecruitmentAccess access;
    private final ApplicationWorkflow workflow;
    private final RbacService rbac;
    private final RecruitmentNotifier notifier;
    private final RecruitmentMailer mailer;

    public InterviewService(NamedParameterJdbcTemplate jdbc, RecruitmentAccess access, ApplicationWorkflow workflow,
                            RbacService rbac, RecruitmentNotifier notifier, RecruitmentMailer mailer) {
        this.mailer = mailer;
        this.jdbc = jdbc;
        this.access = access;
        this.workflow = workflow;
        this.rbac = rbac;
        this.notifier = notifier;
    }

    private static final String SELECT = """
            select i.*, a.code as application_code, cd.full_name as candidate_name, p.title as position_title,
                   s.name as stage_name, u.full_name as created_by_name
            from recruitment_interviews i
            join job_applications a on a.id = i.application_id
            join candidates cd on cd.id = a.candidate_id
            join recruitment_positions p on p.id = a.position_id
            join recruitment_campaigns c on c.id = p.campaign_id
            left join recruitment_stages s on s.id = i.stage_id
            left join users u on u.id = i.created_by
            """;

    public List<Interview> forApplication(Actor actor, UUID applicationId) {
        return query(" where i.application_id = :a order by i.starts_at desc", new MapSqlParameterSource("a", applicationId));
    }

    public record Filter(LocalDateTime from, LocalDateTime to, String status, boolean mine, UUID positionId, UUID storeId) {
    }

    public Page<Interview> list(Actor actor, Filter f, Pageable pageable) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor);
        StringBuilder where = new StringBuilder(" where ").append(RecruitmentAccess.applicationVisible("a", "p", "c"));
        if (f.from() != null) { where.append(" and i.starts_at >= :from"); params.addValue("from", DbTimes.toDb(f.from())); }
        if (f.to() != null) { where.append(" and i.starts_at < :to"); params.addValue("to", DbTimes.toDb(f.to())); }
        if (StringUtils.hasText(f.status())) { where.append(" and i.status = :status"); params.addValue("status", f.status().trim().toUpperCase()); }
        if (f.positionId() != null) { where.append(" and a.position_id = :position"); params.addValue("position", f.positionId()); }
        if (f.storeId() != null) { where.append(" and c.store_id = :filterStore"); params.addValue("filterStore", f.storeId()); }
        if (f.mine()) where.append(" and exists (select 1 from recruitment_interview_participants mp where mp.interview_id = i.id and mp.user_id = :actor)");
        Long total = jdbc.queryForObject("select count(*) from recruitment_interviews i join job_applications a on a.id = i.application_id"
                + " join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id" + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        return new PageImpl<>(query(where + " order by i.starts_at, i.id limit :limit offset :offset", params), pageable,
                total == null ? 0 : total);
    }

    List<Interview> query(String tail, MapSqlParameterSource params) {
        List<Interview> rows = jdbc.query(SELECT + tail, params, (rs, i) -> new Interview(rs.getObject("id", UUID.class),
                rs.getObject("application_id", UUID.class), rs.getString("application_code"), rs.getString("candidate_name"),
                rs.getString("position_title"), rs.getObject("stage_id", UUID.class), rs.getString("stage_name"),
                rs.getString("title"), time(rs, "starts_at"), time(rs, "ends_at"), rs.getString("mode"),
                rs.getString("location"), rs.getString("meeting_url"), rs.getString("status"), rs.getString("note"),
                rs.getString("result_note"), new ArrayList<>(), rs.getString("created_by_name")));
        if (rows.isEmpty()) return rows;
        Map<UUID, List<UserRef>> participants = new HashMap<>();
        jdbc.query("""
                select ip.interview_id, u.id, u.full_name, u.username from recruitment_interview_participants ip
                join users u on u.id = ip.user_id where ip.interview_id in (:ids) order by u.full_name""",
                new MapSqlParameterSource("ids", rows.stream().map(Interview::id).toList()), rs -> {
                    participants.computeIfAbsent(rs.getObject("interview_id", UUID.class), k -> new ArrayList<>())
                            .add(new UserRef(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("username")));
                });
        rows.forEach(r -> r.participants().addAll(participants.getOrDefault(r.id(), List.of())));
        return rows;
    }

    Interview get(UUID id) {
        List<Interview> rows = query(" where i.id = :id", new MapSqlParameterSource("id", id));
        if (rows.isEmpty()) throw BusinessException.notFound("Lịch phỏng vấn không tồn tại");
        return rows.getFirst();
    }

    // ------------------------------------------------------------------ ghi

    @Transactional
    public Interview create(Actor actor, UUID applicationId, InterviewRequest request) {
        access.requireManageApplication(actor, applicationId);
        AppState app = workflow.lock(applicationId);
        ApplicationWorkflow.requireOpen(app);
        UUID stageId = request.stageId() != null ? request.stageId() : app.currentStageId();
        if (stageId == null) throw BusinessException.badRequest("Vị trí chưa cấu hình vòng tuyển dụng");
        StageRow stage = workflow.stage(app.positionId(), stageId);
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from application_stage_results where application_id = :a and stage_id = :s)",
                new MapSqlParameterSource("a", applicationId).addValue("s", stageId), Boolean.class)))
            throw BusinessException.invalidState("Hồ sơ chưa tới vòng \"" + stage.name() + "\"");
        Set<UUID> participants = validate(request, app.positionId(), null);
        UUID id = jdbc.queryForObject("""
                insert into recruitment_interviews (application_id, stage_id, title, starts_at, ends_at, mode, location, meeting_url, note, created_by)
                values (:a, :s, :title, :start, :end, :mode, :location, :url, :note, :actor) returning id""",
                params(request).addValue("a", applicationId).addValue("s", stageId).addValue("actor", actor.userId()), UUID.class);
        saveParticipants(id, applicationId, stageId, participants, actor.userId());
        workflow.history(applicationId, "INTERVIEW", stageId, null, "SCHEDULED",
                "Đặt lịch phỏng vấn \"" + request.title().trim() + "\" lúc " + WHEN.format(request.startsAt()), null, actor.userId());
        if (stageId.equals(app.currentStageId())) {
            workflow.markStageInProgress(applicationId, stageId, StageStatus.IN_PROGRESS, actor.userId(), null);
            if (app.status() != ApplicationStatus.INTERVIEW_PENDING && app.status() != ApplicationStatus.INTERVIEWING)
                workflow.setStatus(app, ApplicationStatus.INTERVIEW_PENDING, actor.userId(), null, null);
        }
        notifyParticipants(participants, actor, applicationId, "Bạn có lịch phỏng vấn mới", request);
        if (!Boolean.FALSE.equals(request.notifyCandidate()))
            mailer.sendTemplate(applicationId, RecruitmentMailer.INTERVIEW_INVITATION, emailVars(get(id)), actor.userId());
        return get(id);
    }

    @Transactional
    public Interview update(Actor actor, UUID id, InterviewRequest request) {
        Interview current = get(id);
        access.requireManageApplication(actor, current.applicationId());
        if (!"SCHEDULED".equals(current.status())) throw BusinessException.invalidState("Chỉ dời / sửa được lịch chưa diễn ra");
        UUID positionId = access.positionOfApplication(current.applicationId());
        Set<UUID> participants = validate(request, positionId, id);
        jdbc.update("""
                update recruitment_interviews set title = :title, starts_at = :start, ends_at = :end, mode = :mode,
                    location = :location, meeting_url = :url, note = :note, updated_at = now() at time zone 'UTC'
                where id = :id""", params(request).addValue("id", id));
        Set<UUID> before = new HashSet<>(current.participants().stream().map(UserRef::id).toList());
        jdbc.update("delete from recruitment_interview_participants where interview_id = :id", new MapSqlParameterSource("id", id));
        saveParticipants(id, current.applicationId(), current.stageId(), participants, actor.userId());
        boolean moved = !current.startsAt().equals(request.startsAt()) || !current.endsAt().equals(request.endsAt());
        workflow.history(current.applicationId(), "INTERVIEW", current.stageId(), null, "SCHEDULED",
                (moved ? "Dời lịch phỏng vấn sang " + WHEN.format(request.startsAt()) : "Cập nhật lịch phỏng vấn")
                        + " (\"" + request.title().trim() + "\")", null, actor.userId());
        notifyParticipants(moved ? participants : participants.stream().filter(u -> !before.contains(u)).toList(),
                actor, current.applicationId(), moved ? "Lịch phỏng vấn đã thay đổi" : "Bạn có lịch phỏng vấn mới", request);
        if (moved && !Boolean.FALSE.equals(request.notifyCandidate()))
            mailer.sendTemplate(current.applicationId(), RecruitmentMailer.INTERVIEW_RESCHEDULED, emailVars(get(id)), actor.userId());
        return get(id);
    }

    /** COMPLETED (kèm nhận xét) → vòng chờ đánh giá; CANCELLED / NO_SHOW chỉ ghi nhận. Người phỏng vấn cũng được ghi kết quả. */
    @Transactional
    public Interview changeStatus(Actor actor, UUID id, String status, String resultNote, boolean notifyCandidate) {
        Interview current = get(id);
        boolean participant = current.participants().stream().anyMatch(u -> u.id().equals(actor.userId()));
        if (!participant && !access.canManageApplication(actor, current.applicationId()))
            throw BusinessException.forbidden("Bạn không phụ trách lịch phỏng vấn này");
        String target = ApplicationWorkflow.requireOneOf(status, "Trạng thái", "COMPLETED", "CANCELLED", "NO_SHOW");
        if (!"SCHEDULED".equals(current.status())) throw BusinessException.invalidState("Lịch phỏng vấn đã được cập nhật kết quả");
        if (!"COMPLETED".equals(target) && !participant && !StringUtils.hasText(resultNote) && "NO_SHOW".equals(target))
            throw BusinessException.badRequest("Vui lòng ghi chú");
        jdbc.update("""
                update recruitment_interviews set status = :status, result_note = :note, updated_at = now() at time zone 'UTC'
                where id = :id""", new MapSqlParameterSource("id", id).addValue("status", target).addValue("note", trim(resultNote)));
        String label = switch (target) {
            case "COMPLETED" -> "Đã phỏng vấn";
            case "CANCELLED" -> "Hủy lịch phỏng vấn";
            default -> "Ứng viên không đến phỏng vấn";
        };
        workflow.history(current.applicationId(), "INTERVIEW", current.stageId(), "SCHEDULED", target,
                label + " \"" + current.title() + "\"", resultNote, actor.userId());
        if ("CANCELLED".equals(target) && notifyCandidate)
            mailer.sendTemplate(current.applicationId(), RecruitmentMailer.INTERVIEW_CANCELLED, emailVars(current), actor.userId());
        AppState app = workflow.lock(current.applicationId());
        if ("COMPLETED".equals(target) && current.stageId() != null && current.stageId().equals(app.currentStageId())
                && !app.status().closed()) {
            workflow.markStageInProgress(app.id(), current.stageId(), StageStatus.AWAITING_EVALUATION, actor.userId(), null);
            Boolean decided = workflow.stageStatus(app.id(), current.stageId()).decided();
            if (!decided) workflow.setStatus(app, ApplicationStatus.EVALUATION_PENDING, actor.userId(), null, null);
        }
        return get(id);
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    /** Biến email về buổi phỏng vấn. */
    static java.util.Map<String, String> emailVars(Interview i) {
        String place = switch (i.mode()) {
            case "ONLINE" -> "Link tham gia: " + (i.meetingUrl() == null ? "" : i.meetingUrl());
            case "PHONE" -> "Bộ phận tuyển dụng sẽ gọi cho bạn qua số điện thoại đã đăng ký.";
            default -> "Địa điểm: " + (i.location() == null ? "" : i.location());
        };
        return java.util.Map.of("interviewTitle", i.title(),
                "interviewTime", HOUR.format(i.startsAt()) + " – " + HOUR.format(i.endsAt()) + " ngày " + DAY.format(i.startsAt()),
                "interviewMode", switch (i.mode()) { case "ONLINE" -> "Phỏng vấn online"; case "PHONE" -> "Phỏng vấn qua điện thoại"; default -> "Phỏng vấn trực tiếp"; },
                "interviewPlace", place);
    }

    private Set<UUID> validate(InterviewRequest r, UUID positionId, UUID excludeInterview) {
        if (!r.endsAt().isAfter(r.startsAt())) throw BusinessException.badRequest("Giờ kết thúc phải sau giờ bắt đầu");
        String mode = ApplicationWorkflow.requireOneOf(r.mode(), "Hình thức", MODES);
        if ("ONLINE".equals(mode) && !StringUtils.hasText(r.meetingUrl()))
            throw BusinessException.badRequest("Phỏng vấn online cần link họp");
        if ("ONSITE".equals(mode) && !StringUtils.hasText(r.location()))
            throw BusinessException.badRequest("Phỏng vấn trực tiếp cần địa điểm");
        UUID store = access.storeOfPosition(positionId);
        Set<UUID> participants = new LinkedHashSet<>(r.participantIds());
        for (UUID userId : participants) {
            access.requireAssignable(userId, store, "Người phỏng vấn");
            Set<String> codes = new HashSet<>();
            for (GrantedAuthority a : rbac.loadAuthorities(userId)) codes.add(a.getAuthority());
            if (!codes.contains("RECRUITMENT_EVALUATE") && !codes.contains("RECRUITMENT_ADMIN"))
                throw BusinessException.badRequest("Nhân viên được chọn chưa có quyền đánh giá ứng viên (vai trò Người phỏng vấn)");
        }
        // Trùng lịch của người phỏng vấn.
        List<String> busy = jdbc.queryForList("""
                select distinct u.full_name from recruitment_interviews i
                join recruitment_interview_participants ip on ip.interview_id = i.id join users u on u.id = ip.user_id
                where ip.user_id in (:users) and i.status = 'SCHEDULED' and i.starts_at < :end and i.ends_at > :start
                  and (cast(:exclude as uuid) is null or i.id <> :exclude)""",
                new MapSqlParameterSource("users", participants).addValue("start", DbTimes.toDb(r.startsAt()))
                        .addValue("end", DbTimes.toDb(r.endsAt())).addValue("exclude", excludeInterview, java.sql.Types.OTHER), String.class);
        if (!busy.isEmpty()) throw BusinessException.conflict("Trùng lịch phỏng vấn khác: " + String.join(", ", busy));
        return participants;
    }

    private MapSqlParameterSource params(InterviewRequest r) {
        return new MapSqlParameterSource("title", r.title().trim()).addValue("start", DbTimes.toDb(r.startsAt()))
                .addValue("end", DbTimes.toDb(r.endsAt())).addValue("mode", r.mode().trim().toUpperCase())
                .addValue("location", trim(r.location())).addValue("url", trim(r.meetingUrl())).addValue("note", trim(r.note()));
    }

    /** Người phỏng vấn cũng là người đánh giá của vòng đó. */
    private void saveParticipants(UUID interviewId, UUID applicationId, UUID stageId, Set<UUID> users, UUID actor) {
        for (UUID userId : users) {
            jdbc.update("insert into recruitment_interview_participants (interview_id, user_id) values (:i, :u) on conflict do nothing",
                    new MapSqlParameterSource("i", interviewId).addValue("u", userId));
            if (stageId != null) {
                jdbc.update("""
                        insert into application_stage_evaluators (application_id, stage_id, user_id, assigned_by)
                        values (:a, :s, :u, :by) on conflict do nothing""",
                        new MapSqlParameterSource("a", applicationId).addValue("s", stageId).addValue("u", userId).addValue("by", actor));
            }
        }
    }

    private void notifyParticipants(java.util.Collection<UUID> users, Actor actor, UUID applicationId, String title, InterviewRequest r) {
        String candidate = jdbc.queryForObject("""
                select cd.full_name || ' – ' || p.title from job_applications a join candidates cd on cd.id = a.candidate_id
                join recruitment_positions p on p.id = a.position_id where a.id = :a""",
                new MapSqlParameterSource("a", applicationId), String.class);
        notifier.notifyUsers(users, actor.userId(), title, candidate + ", " + WHEN.format(r.startsAt()), applicationId);
    }
}
