package com.fashionsystem.fashion_system.recruitment;

import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.time;
import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.trim;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.AppState;
import com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.StageRow;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluationTask;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluatorView;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.HistoryEntry;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Option;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Stage;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.StageProgress;
import com.fashionsystem.fashion_system.service.RbacService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
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

/** Hồ sơ ứng tuyển: danh sách, chi tiết (tiến độ từng vòng + lịch sử), đổi trạng thái, chuyển vòng, phân công đánh giá. */
@Service
@Transactional(readOnly = true)
public class ApplicationService {
    private final NamedParameterJdbcTemplate jdbc;
    private final RecruitmentAccess access;
    private final ApplicationWorkflow workflow;
    private final PipelineService pipelines;
    private final InterviewService interviews;
    private final OfferService offers;
    private final CandidateDocumentService documents;
    private final RbacService rbac;
    private final RecruitmentNotifier notifier;
    private final RecruitmentMailer mailer;

    public ApplicationService(NamedParameterJdbcTemplate jdbc, RecruitmentAccess access, ApplicationWorkflow workflow,
                              PipelineService pipelines, InterviewService interviews, OfferService offers,
                              CandidateDocumentService documents, RbacService rbac, RecruitmentNotifier notifier,
                              RecruitmentMailer mailer) {
        this.mailer = mailer;
        this.jdbc = jdbc;
        this.access = access;
        this.workflow = workflow;
        this.pipelines = pipelines;
        this.interviews = interviews;
        this.offers = offers;
        this.documents = documents;
        this.rbac = rbac;
        this.notifier = notifier;
    }

    // ------------------------------------------------------------------ đọc

    static final String SUMMARY_SELECT = """
            select a.id, a.code, a.candidate_id, cd.code as candidate_code, cd.full_name as candidate_name,
                   cd.email as candidate_email, cd.phone as candidate_phone, a.position_id, p.title as position_title,
                   p.campaign_id, c.name as campaign_name, a.status, a.current_stage_id, s.name as current_stage_name,
                   r.status as current_stage_status,
                   (select count(*) from recruitment_stages x where x.position_id = p.id and x.sort_order <= s.sort_order) as stage_index,
                   (select count(*) from recruitment_stages x where x.position_id = p.id) as stage_count,
                   (select r2.score from application_stage_results r2 join recruitment_stages s2 on s2.id = r2.stage_id
                    where r2.application_id = a.id and r2.score is not null order by s2.sort_order desc limit 1) as last_score,
                   a.source, a.applied_at, a.updated_at
            from job_applications a
            join candidates cd on cd.id = a.candidate_id
            join recruitment_positions p on p.id = a.position_id
            join recruitment_campaigns c on c.id = p.campaign_id
            left join recruitment_stages s on s.id = a.current_stage_id
            left join application_stage_results r on r.application_id = a.id and r.stage_id = a.current_stage_id
            """;

    static ApplicationSummary summary(ResultSet rs) throws SQLException {
        ApplicationStatus status = ApplicationStatus.valueOf(rs.getString("status"));
        return new ApplicationSummary(rs.getObject("id", UUID.class), rs.getString("code"),
                rs.getObject("candidate_id", UUID.class), rs.getString("candidate_code"), rs.getString("candidate_name"),
                rs.getString("candidate_email"), rs.getString("candidate_phone"), rs.getObject("position_id", UUID.class),
                rs.getString("position_title"), rs.getObject("campaign_id", UUID.class), rs.getString("campaign_name"),
                status.name(), status.label(), rs.getObject("current_stage_id", UUID.class), rs.getString("current_stage_name"),
                rs.getString("current_stage_status"), rs.getInt("stage_index"), rs.getInt("stage_count"),
                rs.getBigDecimal("last_score"), rs.getString("source"), time(rs, "applied_at"), time(rs, "updated_at"));
    }

    public record Filter(UUID campaignId, UUID positionId, UUID candidateId, UUID stageId, String status, String keyword,
                         String source, UUID storeId, boolean assignedToMe) {
    }

    public Page<ApplicationSummary> list(Actor actor, Filter f, Pageable pageable) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor);
        StringBuilder where = new StringBuilder(" where ").append(RecruitmentAccess.applicationVisible("a", "p", "c"));
        if (f.campaignId() != null) { where.append(" and p.campaign_id = :campaign"); params.addValue("campaign", f.campaignId()); }
        if (f.positionId() != null) { where.append(" and a.position_id = :position"); params.addValue("position", f.positionId()); }
        if (f.candidateId() != null) { where.append(" and a.candidate_id = :candidate"); params.addValue("candidate", f.candidateId()); }
        if (f.stageId() != null) { where.append(" and a.current_stage_id = :stage"); params.addValue("stage", f.stageId()); }
        if (f.storeId() != null) { where.append(" and c.store_id = :filterStore"); params.addValue("filterStore", f.storeId()); }
        if (StringUtils.hasText(f.status())) {
            List<String> statuses = new ArrayList<>();
            for (String s : f.status().split(",")) statuses.add(ApplicationWorkflow.parse(ApplicationStatus.class, s, "Trạng thái").name());
            where.append(" and a.status in (:statuses)");
            params.addValue("statuses", statuses);
        }
        if (StringUtils.hasText(f.source())) { where.append(" and a.source = :source"); params.addValue("source", f.source().trim().toUpperCase()); }
        if (StringUtils.hasText(f.keyword())) {
            where.append(" and (cd.full_name ilike :kw or cd.email ilike :kw or cd.phone ilike :kw or a.code ilike :kw or cd.code ilike :kw)");
            params.addValue("kw", "%" + f.keyword().trim() + "%");
        }
        if (f.assignedToMe()) {
            where.append("""
                     and (exists (select 1 from application_stage_evaluators e where e.application_id = a.id and e.user_id = :actor)
                          or exists (select 1 from recruitment_interviews i join recruitment_interview_participants ip on ip.interview_id = i.id
                                     where i.application_id = a.id and ip.user_id = :actor))""");
        }
        String from = " from job_applications a join candidates cd on cd.id = a.candidate_id"
                + " join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id";
        Long total = jdbc.queryForObject("select count(*)" + from + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<ApplicationSummary> rows = jdbc.query(SUMMARY_SELECT + where
                + " order by coalesce(a.updated_at, a.applied_at) desc, a.id limit :limit offset :offset", params, (rs, i) -> summary(rs));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    ApplicationSummary summary(UUID id) {
        List<ApplicationSummary> rows = jdbc.query(SUMMARY_SELECT + " where a.id = :id", new MapSqlParameterSource("id", id), (rs, i) -> summary(rs));
        if (rows.isEmpty()) throw BusinessException.notFound("Hồ sơ ứng tuyển không tồn tại");
        return rows.getFirst();
    }

    public ApplicationDetail detail(Actor actor, UUID id) {
        access.requireViewApplication(actor, id);
        ApplicationSummary app = summary(id);
        boolean canManage = access.canManageApplication(actor, id);
        boolean canApprove = access.canApprovePosition(actor, app.positionId());
        ApplicationStatus status = ApplicationStatus.valueOf(app.status());
        List<Option> allowed = canManage ? status.allowedNext().stream().filter(s -> !s.systemOnly())
                .map(s -> new Option(s.name(), s.label())).toList() : List.of();
        record Extra(String coverNote, String closedReason, UUID hiredEmployeeId, java.time.LocalDateTime hiredAt, String employeeCode) {
        }
        Extra extra = jdbc.query("""
                select a.cover_note, a.closed_reason, a.hired_employee_id, a.hired_at, u.employee_code
                from job_applications a left join users u on u.id = a.hired_employee_id where a.id = :id""",
                new MapSqlParameterSource("id", id), (rs, i) -> new Extra(rs.getString("cover_note"), rs.getString("closed_reason"),
                        rs.getObject("hired_employee_id", UUID.class), time(rs, "hired_at"), rs.getString("employee_code"))).getFirst();
        boolean seesAll = canManage || canApprove;
        return new ApplicationDetail(app, extra.coverNote(), extra.closedReason(), extra.hiredEmployeeId(), extra.hiredAt(),
                allowed, stages(actor, id, app.positionId()), interviews.forApplication(actor, id),
                seesAll ? offers.forApplication(actor, id) : List.of(), history(id), documents.list(app.candidateId()),
                seesAll ? mailer.logs(id) : List.of(), extra.employeeCode(), canManage, canApprove);
    }

    private List<StageProgress> stages(Actor actor, UUID applicationId, UUID positionId) {
        MapSqlParameterSource params = new MapSqlParameterSource("a", applicationId).addValue("actor", actor.userId());
        record Result(String status, java.math.BigDecimal score, String decidedBy, java.time.LocalDateTime decidedAt,
                      String note, java.time.LocalDateTime startedAt) {
        }
        Map<UUID, Result> results = new HashMap<>();
        jdbc.query("""
                select r.*, u.full_name as decided_by_name from application_stage_results r
                left join users u on u.id = r.decided_by where r.application_id = :a""", params, rs -> {
            results.put(rs.getObject("stage_id", UUID.class), new Result(rs.getString("status"), rs.getBigDecimal("score"),
                    rs.getString("decided_by_name"), time(rs, "decided_at"), rs.getString("decision_note"), time(rs, "started_at")));
        });
        // Người được phân công + người đã có phiếu (kể cả không được phân công, ví dụ hiring manager).
        Map<UUID, List<EvaluatorView>> evaluators = new HashMap<>();
        jdbc.query("""
                select x.stage_id, x.user_id, u.full_name, e.status, e.total_score, e.recommendation, e.submitted_at
                from (select stage_id, user_id from application_stage_evaluators where application_id = :a
                      union select stage_id, evaluator_id from application_evaluations where application_id = :a) x
                join users u on u.id = x.user_id
                left join application_evaluations e on e.application_id = :a and e.stage_id = x.stage_id and e.evaluator_id = x.user_id
                order by u.full_name""", params, rs -> {
            evaluators.computeIfAbsent(rs.getObject("stage_id", UUID.class), k -> new ArrayList<>()).add(new EvaluatorView(
                    rs.getObject("user_id", UUID.class), rs.getString("full_name"),
                    rs.getString("status") == null ? "NOT_STARTED" : rs.getString("status"), rs.getBigDecimal("total_score"),
                    rs.getString("recommendation"), time(rs, "submitted_at")));
        });
        List<StageProgress> progress = new ArrayList<>();
        for (Stage stage : pipelines.stages(positionId)) {
            Result r = results.get(stage.id());
            StageStatus status = r == null ? null : StageStatus.valueOf(r.status());
            List<EvaluatorView> views = evaluators.getOrDefault(stage.id(), List.of());
            String mine = views.stream().filter(v -> v.userId().equals(actor.userId())).map(EvaluatorView::evaluationStatus)
                    .findFirst().orElse(null);
            boolean canEvaluate = r != null && access.canEvaluate(actor, applicationId, stage.id());
            progress.add(new StageProgress(stage, status == null ? "NOT_REACHED" : status.name(),
                    status == null ? "Chưa tới" : status.label(), r == null ? null : r.score(), r == null ? null : r.decidedBy(),
                    r == null ? null : r.decidedAt(), r == null ? null : r.note(), r == null ? null : r.startedAt(),
                    views, mine, canEvaluate));
        }
        return progress;
    }

    List<HistoryEntry> history(UUID applicationId) {
        return jdbc.query("""
                select h.*, s.name as stage_name, u.full_name as actor_name from application_history h
                left join recruitment_stages s on s.id = h.stage_id left join users u on u.id = h.actor_id
                where h.application_id = :a order by h.created_at desc, h.id""", new MapSqlParameterSource("a", applicationId),
                (rs, i) -> new HistoryEntry(rs.getObject("id", UUID.class), rs.getString("kind"), rs.getString("stage_name"),
                        rs.getString("from_status"), rs.getString("to_status"), rs.getString("message"), rs.getString("reason"),
                        rs.getString("actor_name"), time(rs, "created_at")));
    }

    /** Việc đánh giá đang chờ người dùng (được phân công, vòng chưa có kết luận, phiếu chưa nộp). */
    public List<EvaluationTask> myTasks(Actor actor) {
        return jdbc.query("""
                select a.id as application_id, a.code, cd.full_name, p.title, s.id as stage_id, s.name as stage_name,
                       coalesce(e.status, 'NOT_STARTED') as evaluation_status,
                       (select min(i.starts_at) from recruitment_interviews i join recruitment_interview_participants ip on ip.interview_id = i.id
                        where i.application_id = a.id and i.stage_id = s.id and ip.user_id = :actor and i.status = 'SCHEDULED') as next_interview,
                       x.assigned_at
                from (select application_id, stage_id, user_id, assigned_at from application_stage_evaluators where user_id = :actor) x
                join job_applications a on a.id = x.application_id
                join candidates cd on cd.id = a.candidate_id
                join recruitment_positions p on p.id = a.position_id
                join recruitment_stages s on s.id = x.stage_id
                join application_stage_results r on r.application_id = a.id and r.stage_id = s.id
                left join application_evaluations e on e.application_id = a.id and e.stage_id = s.id and e.evaluator_id = :actor
                where r.status in ('PENDING', 'IN_PROGRESS', 'AWAITING_EVALUATION')
                  and a.status not in ('REJECTED', 'WITHDRAWN', 'HIRED', 'OFFER_DECLINED')
                  and coalesce(e.status, 'DRAFT') <> 'SUBMITTED'
                order by next_interview nulls last, x.assigned_at""", new MapSqlParameterSource("actor", actor.userId()),
                (rs, i) -> new EvaluationTask(rs.getObject("application_id", UUID.class), rs.getString("code"),
                        rs.getString("full_name"), rs.getString("title"), rs.getObject("stage_id", UUID.class),
                        rs.getString("stage_name"), rs.getString("evaluation_status"), time(rs, "next_interview"),
                        time(rs, "assigned_at")));
    }

    // ------------------------------------------------------------------ ghi

    @Transactional
    public ApplicationDetail create(Actor actor, ApplicationRequest request) {
        access.requireManagePosition(actor, request.positionId());
        requireCandidateInScope(actor, request.candidateId());
        UUID id = createInternal(request.candidateId(), request.positionId(), request.source(), request.coverNote(),
                actor.userId(), false, Boolean.TRUE.equals(request.notifyCandidate()));
        return detail(actor, id);
    }

    private void requireCandidateInScope(Actor actor, UUID candidateId) {
        access.requireViewCandidate(actor, candidateId);
    }

    /**
     * Tạo hồ sơ ứng tuyển và đưa vào vòng đầu tiên.
     *
     * @param publicForm true: từ biểu mẫu công khai (vị trí phải đang mở và còn hạn)
     */
    UUID createInternal(UUID candidateId, UUID positionId, String source, String coverNote, UUID actor, boolean publicForm,
                        boolean notifyCandidate) {
        record Pos(String status, String campaignStatus, java.time.LocalDate deadline) {
        }
        List<Pos> pos = jdbc.query("""
                select p.status, c.status as campaign_status, p.deadline from recruitment_positions p
                join recruitment_campaigns c on c.id = p.campaign_id where p.id = :id for update of p""",
                new MapSqlParameterSource("id", positionId), (rs, i) -> new Pos(rs.getString("status"),
                        rs.getString("campaign_status"), rs.getObject("deadline", java.time.LocalDate.class)));
        if (pos.isEmpty()) throw BusinessException.notFound("Vị trí tuyển dụng không tồn tại");
        Pos p = pos.getFirst();
        if (publicForm) {
            if (!"OPEN".equals(p.status()) || !"OPEN".equals(p.campaignStatus())
                    || (p.deadline() != null && p.deadline().isBefore(java.time.LocalDate.now())))
                throw BusinessException.invalidState("Vị trí này đã ngừng nhận hồ sơ");
        } else if (List.of("CLOSED", "FILLED").contains(p.status()) || List.of("CLOSED", "CANCELLED").contains(p.campaignStatus())) {
            throw BusinessException.invalidState("Vị trí hoặc chiến dịch đã đóng, không nhận thêm hồ sơ");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from job_applications where candidate_id = :c and position_id = :p)",
                new MapSqlParameterSource("c", candidateId).addValue("p", positionId), Boolean.class)))
            throw BusinessException.conflict(publicForm ? "Bạn đã ứng tuyển vị trí này rồi" : "Ứng viên đã có hồ sơ cho vị trí này");
        String src = StringUtils.hasText(source) ? source.trim().toUpperCase() : (publicForm ? "WEBSITE" : "OTHER");
        if (!RecruitmentDtos.SOURCES.contains(src)) throw BusinessException.badRequest("Nguồn ứng tuyển không hợp lệ");
        UUID id = jdbc.queryForObject("""
                insert into job_applications (code, candidate_id, position_id, status, source, cover_note, created_by)
                values (:code, :c, :p, 'NEW', :source, :note, :actor) returning id""",
                new MapSqlParameterSource("code", workflow.nextCode("HS", "job_application_code_seq")).addValue("c", candidateId)
                        .addValue("p", positionId).addValue("source", src).addValue("note", trim(coverNote)).addValue("actor", actor),
                UUID.class);
        workflow.start(id, actor);
        if (notifyCandidate) mailer.sendTemplate(id, RecruitmentMailer.APPLICATION_RECEIVED, null, actor);
        return id;
    }

    @Transactional
    public ApplicationDetail changeStatus(Actor actor, UUID id, String status, String reason, boolean notifyCandidate) {
        access.requireManageApplication(actor, id);
        AppState app = workflow.lock(id);
        ApplicationStatus target = ApplicationWorkflow.parse(ApplicationStatus.class, status, "Trạng thái");
        workflow.changeStatusManually(app, target, actor.userId(), reason);
        // Lý do nội bộ không gửi cho ứng viên; email dùng mẫu "Thông báo kết quả".
        if (target == ApplicationStatus.REJECTED && notifyCandidate)
            mailer.sendTemplate(id, RecruitmentMailer.REJECTION, null, actor.userId());
        return detail(actor, id);
    }

    /** Kết luận vòng thủ công (vòng MANUAL hoặc ghi đè kết quả tự động): người quản lý hồ sơ hoặc người duyệt. */
    @Transactional
    public ApplicationDetail decideStage(Actor actor, UUID id, UUID stageId, String decision, String note) {
        UUID positionId = access.positionOfApplication(id);
        if (!access.canManagePosition(actor, positionId) && !access.canApprovePosition(actor, positionId))
            throw BusinessException.forbidden("Bạn không có quyền kết luận vòng này");
        StageStatus outcome = ApplicationWorkflow.parse(StageStatus.class, decision, "Kết quả");
        if (!outcome.decided()) throw BusinessException.badRequest("Kết quả phải là Đạt, Không đạt hoặc Bỏ qua");
        AppState app = workflow.lock(id);
        ApplicationWorkflow.requireOpen(app);
        StageRow stage = workflow.stage(app.positionId(), stageId);
        if (workflow.stageStatus(id, stageId) == StageStatus.PENDING && !stageId.equals(app.currentStageId())
                && !Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from application_stage_results where application_id = :a and stage_id = :s)",
                new MapSqlParameterSource("a", id).addValue("s", stageId), Boolean.class)))
            throw BusinessException.invalidState("Hồ sơ chưa tới vòng này");
        if (outcome == StageStatus.FAILED && !StringUtils.hasText(note))
            throw BusinessException.badRequest("Vui lòng nhập lý do không đạt");
        java.math.BigDecimal average = jdbc.queryForObject("""
                select round(avg(total_score), 2) from application_evaluations
                where application_id = :a and stage_id = :s and status = 'SUBMITTED'""",
                new MapSqlParameterSource("a", id).addValue("s", stageId), java.math.BigDecimal.class);
        workflow.decideStage(app, stage, outcome, average, actor.userId(), note, false);
        return detail(actor, id);
    }

    @Transactional
    public ApplicationDetail advance(Actor actor, UUID id, String note) {
        access.requireManageApplication(actor, id);
        workflow.advance(workflow.lock(id), actor.userId(), note);
        return detail(actor, id);
    }

    @Transactional
    public ApplicationDetail moveToStage(Actor actor, UUID id, UUID stageId, String note) {
        access.requireManageApplication(actor, id);
        AppState app = workflow.lock(id);
        workflow.moveTo(app, workflow.stage(app.positionId(), stageId), actor.userId(), note);
        return detail(actor, id);
    }

    /** Phân công người đánh giá cho hồ sơ ở một vòng (thay toàn bộ danh sách; người đã nộp phiếu luôn được giữ lại). */
    @Transactional
    public ApplicationDetail assignEvaluators(Actor actor, UUID id, UUID stageId, List<UUID> userIds) {
        access.requireManageApplication(actor, id);
        AppState app = workflow.lock(id);
        StageRow stage = workflow.stage(app.positionId(), stageId);
        UUID store = access.storeOfPosition(app.positionId());
        Set<UUID> wanted = new LinkedHashSet<>(userIds);
        for (UUID userId : wanted) {
            access.requireAssignable(userId, store, "Người đánh giá");
            Set<String> codes = new java.util.HashSet<>();
            for (GrantedAuthority a : rbac.loadAuthorities(userId)) codes.add(a.getAuthority());
            if (!codes.contains("RECRUITMENT_EVALUATE") && !codes.contains("RECRUITMENT_ADMIN"))
                throw BusinessException.badRequest("Nhân viên được chọn chưa có quyền đánh giá ứng viên (vai trò Người phỏng vấn / Hiring Manager)");
        }
        List<UUID> before = jdbc.queryForList("select user_id from application_stage_evaluators where application_id = :a and stage_id = :s",
                new MapSqlParameterSource("a", id).addValue("s", stageId), UUID.class);
        jdbc.update("""
                delete from application_stage_evaluators where application_id = :a and stage_id = :s
                  and user_id not in (select evaluator_id from application_evaluations
                                      where application_id = :a and stage_id = :s and status = 'SUBMITTED')
                  and (cardinality(cast(:keep as uuid[])) = 0 or user_id <> all(cast(:keep as uuid[])))""",
                new MapSqlParameterSource("a", id).addValue("s", stageId).addValue("keep", wanted.toArray(UUID[]::new)));
        for (UUID userId : wanted) {
            jdbc.update("""
                    insert into application_stage_evaluators (application_id, stage_id, user_id, assigned_by)
                    values (:a, :s, :u, :by) on conflict do nothing""",
                    new MapSqlParameterSource("a", id).addValue("s", stageId).addValue("u", userId).addValue("by", actor.userId()));
        }
        List<UUID> added = wanted.stream().filter(u -> !before.contains(u)).toList();
        if (!added.isEmpty() || before.stream().anyMatch(u -> !wanted.contains(u))) {
            List<String> names = jdbc.queryForList("""
                    select u.full_name from application_stage_evaluators e join users u on u.id = e.user_id
                    where e.application_id = :a and e.stage_id = :s order by u.full_name""",
                    new MapSqlParameterSource("a", id).addValue("s", stageId), String.class);
            workflow.history(id, "EVALUATION", stageId, null, null, "Người đánh giá vòng \"" + stage.name() + "\": "
                    + (names.isEmpty() ? "(chưa có)" : String.join(", ", names)), null, actor.userId());
            ApplicationSummary s = summary(id);
            notifier.notifyUsers(added, actor.userId(), "Bạn được phân công đánh giá ứng viên",
                    s.candidateName() + " – " + s.positionTitle() + " (vòng " + stage.name() + ")", id);
        }
        return detail(actor, id);
    }

    @Transactional
    public ApplicationDetail addNote(Actor actor, UUID id, String message) {
        access.requireViewApplication(actor, id);
        workflow.history(id, "NOTE", null, null, null, message.trim(), null, actor.userId());
        return detail(actor, id);
    }
}
