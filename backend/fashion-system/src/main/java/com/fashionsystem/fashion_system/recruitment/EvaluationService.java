package com.fashionsystem.fashion_system.recruitment;

import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.time;
import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.trim;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.AppState;
import com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.StageRow;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Criterion;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Evaluation;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluationForm;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EvaluationRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ScoreInput;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ScoreView;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Stage;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Decision;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Mark;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Outcome;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.PassRule;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.Recommendation;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.ScoringMode;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.SheetResult;
import com.fashionsystem.fashion_system.recruitment.RecruitmentScoring.SubmittedSheet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu đánh giá: mỗi người một phiếu cho mỗi hồ sơ ở mỗi vòng, chấm điểm hoặc Đạt/Không đạt theo từng tiêu chí, kèm
 * nhận xét và đề xuất. Nộp phiếu thì khóa; hệ thống tính điểm tổng hợp và tự kết luận vòng theo cấu hình
 * ({@link RecruitmentScoring}). Người đánh giá chỉ thấy phiếu của người khác sau khi đã nộp phiếu của mình (tránh thiên
 * kiến); người quản lý / duyệt thấy tất cả.
 */
@Service
@Transactional(readOnly = true)
public class EvaluationService {
    private final NamedParameterJdbcTemplate jdbc;
    private final RecruitmentAccess access;
    private final ApplicationWorkflow workflow;
    private final PipelineService pipelines;
    private final ApplicationService applications;

    public EvaluationService(NamedParameterJdbcTemplate jdbc, RecruitmentAccess access, ApplicationWorkflow workflow,
                             PipelineService pipelines, ApplicationService applications) {
        this.jdbc = jdbc;
        this.access = access;
        this.workflow = workflow;
        this.pipelines = pipelines;
        this.applications = applications;
    }

    public EvaluationForm form(Actor actor, UUID applicationId, UUID stageId) {
        UUID positionId = access.positionOfApplication(applicationId);
        boolean supervisor = access.canManagePosition(actor, positionId) || access.canApprovePosition(actor, positionId);
        boolean canEvaluate = access.canEvaluate(actor, applicationId, stageId);
        if (!supervisor && !canEvaluate) throw BusinessException.forbidden("Bạn không được phân công đánh giá hồ sơ này");
        Stage stage = stageOf(positionId, stageId);
        ApplicationSummary app = applications.summary(applicationId);
        List<Evaluation> all = evaluations(applicationId, stageId);
        Evaluation mine = all.stream().filter(e -> e.evaluatorId().equals(actor.userId())).findFirst().orElse(null);
        boolean mineSubmitted = mine != null && "SUBMITTED".equals(mine.status());
        boolean showOthers = supervisor || mineSubmitted;
        List<Evaluation> others = all.stream().filter(e -> !e.evaluatorId().equals(actor.userId()))
                .filter(e -> "SUBMITTED".equals(e.status())).toList();
        StageStatus status = workflow.stageStatus(applicationId, stageId);
        boolean reached = Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from application_stage_results where application_id = :a and stage_id = :s)",
                new MapSqlParameterSource("a", applicationId).addValue("s", stageId), Boolean.class));
        List<BigDecimal> scoreRows = jdbc.query("select score from application_stage_results where application_id = :a and stage_id = :s",
                new MapSqlParameterSource("a", applicationId).addValue("s", stageId), (rs, i) -> rs.getBigDecimal("score"));
        BigDecimal stageScore = scoreRows.isEmpty() ? null : scoreRows.getFirst();
        boolean locked = !reached || status.decided() || ApplicationStatus.valueOf(app.status()).closed() || mineSubmitted;
        return new EvaluationForm(app, stage, reached ? status.name() : "NOT_REACHED", stageScore, mine,
                showOthers ? others : List.of(), !showOthers && !others.isEmpty(), canEvaluate, locked);
    }

    @Transactional
    public EvaluationForm save(Actor actor, UUID applicationId, UUID stageId, EvaluationRequest request) {
        if (!access.canEvaluate(actor, applicationId, stageId))
            throw BusinessException.forbidden("Bạn không được phân công đánh giá hồ sơ này");
        AppState app = workflow.lock(applicationId);
        ApplicationWorkflow.requireOpen(app);
        StageRow stageRow = workflow.stage(app.positionId(), stageId);
        Stage stage = stageOf(app.positionId(), stageId);
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from application_stage_results where application_id = :a and stage_id = :s)",
                new MapSqlParameterSource("a", applicationId).addValue("s", stageId), Boolean.class)))
            throw BusinessException.invalidState("Hồ sơ chưa tới vòng \"" + stage.name() + "\"");
        if (workflow.stageStatus(applicationId, stageId).decided())
            throw BusinessException.invalidState("Vòng \"" + stage.name() + "\" đã có kết luận, không thể chấm thêm");
        List<UUID> existing = jdbc.queryForList("""
                select id from application_evaluations where application_id = :a and stage_id = :s and evaluator_id = :u
                  and status = 'SUBMITTED'""", new MapSqlParameterSource("a", applicationId).addValue("s", stageId)
                .addValue("u", actor.userId()), UUID.class);
        if (!existing.isEmpty()) throw BusinessException.invalidState("Bạn đã nộp phiếu đánh giá cho vòng này");

        boolean submit = Boolean.TRUE.equals(request.submit());
        ScoringMode mode = ScoringMode.valueOf(stage.scoringMode());
        Map<UUID, Criterion> criteria = new HashMap<>();
        stage.criteria().forEach(c -> criteria.put(c.id(), c));
        Map<UUID, ScoreInput> inputs = new HashMap<>();
        for (ScoreInput in : request.scores()) {
            if (!criteria.containsKey(in.criterionId())) throw BusinessException.badRequest("Tiêu chí không thuộc vòng này");
            Criterion c = criteria.get(in.criterionId());
            if (in.score() != null && in.score().compareTo(c.maxScore()) > 0)
                throw BusinessException.badRequest("Điểm \"" + c.name() + "\" tối đa " + c.maxScore().stripTrailingZeros().toPlainString());
            inputs.put(in.criterionId(), in);
        }
        // Đủ ô chấm thì tính điểm (cả khi lưu nháp, để người chấm thấy trước).
        BigDecimal total = null;
        boolean criticalFailed = false;
        boolean complete = stage.criteria().stream().allMatch(c -> {
            ScoreInput in = inputs.get(c.id());
            return in != null && (mode == ScoringMode.PASS_FAIL ? in.passed() != null : in.score() != null);
        });
        if (complete && !stage.criteria().isEmpty()) {
            SheetResult sheet;
            try {
                sheet = RecruitmentScoring.sheet(mode, stage.scaleMax(), stage.criteria().stream().map(c -> new Mark(
                        new RecruitmentScoring.Criterion(c.maxScore(), c.weight(), "CRITICAL".equals(c.importance())),
                        inputs.get(c.id()).score(), inputs.get(c.id()).passed())).toList());
            } catch (IllegalArgumentException e) {
                throw BusinessException.badRequest(e.getMessage());
            }
            total = sheet.total();
            criticalFailed = sheet.criticalFailed();
        }
        Recommendation recommendation = request.recommendation() == null || request.recommendation().isBlank() ? null
                : ApplicationWorkflow.parse(Recommendation.class, request.recommendation(), "Đề xuất");
        if (submit) {
            if (!complete) throw BusinessException.badRequest("Vui lòng chấm đủ mọi tiêu chí trước khi nộp phiếu");
            if (recommendation == null) recommendation = derivedRecommendation(stage, total, criticalFailed);
            if (recommendation == null) throw BusinessException.badRequest("Vui lòng chọn đề xuất Đạt / Không đạt");
        }

        UUID evaluationId = jdbc.queryForObject("""
                insert into application_evaluations (application_id, stage_id, evaluator_id, status, total_score, recommendation,
                    comment, critical_failed, submitted_at)
                values (:a, :s, :u, :status, :total, :rec, :comment, :critical,
                        case when :submit then now() at time zone 'UTC' end)
                on conflict (application_id, stage_id, evaluator_id) do update set status = :status, total_score = :total,
                    recommendation = :rec, comment = :comment, critical_failed = :critical, updated_at = now() at time zone 'UTC',
                    submitted_at = case when :submit then now() at time zone 'UTC' end
                returning id""", new MapSqlParameterSource("a", applicationId).addValue("s", stageId).addValue("u", actor.userId())
                .addValue("status", submit ? "SUBMITTED" : "DRAFT").addValue("total", total)
                .addValue("rec", recommendation == null ? null : recommendation.name()).addValue("comment", trim(request.comment()))
                .addValue("critical", criticalFailed).addValue("submit", submit), UUID.class);
        jdbc.update("delete from application_evaluation_scores where evaluation_id = :e", new MapSqlParameterSource("e", evaluationId));
        for (ScoreInput in : inputs.values()) {
            jdbc.update("""
                    insert into application_evaluation_scores (evaluation_id, criterion_id, score, passed, comment)
                    values (:e, :c, :score, :passed, :comment)""", new MapSqlParameterSource("e", evaluationId)
                    .addValue("c", in.criterionId()).addValue("score", mode == ScoringMode.SCORE ? in.score() : null)
                    .addValue("passed", mode == ScoringMode.PASS_FAIL ? in.passed() : null)
                    .addValue("comment", ApplicationWorkflow.cut(trim(in.comment()), 1000)));
        }
        // Người chấm không nằm trong danh sách phân công (hiring manager, người phụ trách) vẫn được tính vào vòng.
        jdbc.update("""
                insert into application_stage_evaluators (application_id, stage_id, user_id, assigned_by)
                values (:a, :s, :u, :u) on conflict do nothing""",
                new MapSqlParameterSource("a", applicationId).addValue("s", stageId).addValue("u", actor.userId()));
        workflow.markStageInProgress(applicationId, stageId, StageStatus.IN_PROGRESS, actor.userId(), null);
        if (submit) {
            String scoreText = total == null ? "" : " – " + total.stripTrailingZeros().toPlainString() + "/"
                    + stage.scaleMax().stripTrailingZeros().toPlainString();
            workflow.history(applicationId, "EVALUATION", stageId, null, recommendation.name(),
                    "Nộp phiếu đánh giá vòng \"" + stage.name() + "\": " + label(recommendation) + scoreText,
                    criticalFailed ? "Không đạt tiêu chí bắt buộc" : null, actor.userId());
            tryDecide(workflow.lock(applicationId), stageRow, stage, actor.userId());
        }
        return form(actor, applicationId, stageId);
    }

    /** Đủ phiếu theo cấu hình → kết luận tự động; chưa đủ / vòng thủ công → hồ sơ "Chờ đánh giá". */
    private void tryDecide(AppState app, StageRow stageRow, Stage stage, UUID actor) {
        MapSqlParameterSource params = new MapSqlParameterSource("a", app.id()).addValue("s", stage.id());
        List<SubmittedSheet> sheets = jdbc.query("""
                select total_score, recommendation, critical_failed from application_evaluations
                where application_id = :a and stage_id = :s and status = 'SUBMITTED'""", params,
                (rs, i) -> new SubmittedSheet(rs.getBigDecimal("total_score"), Recommendation.valueOf(rs.getString("recommendation")),
                        rs.getBoolean("critical_failed")));
        Integer pending = jdbc.queryForObject("""
                select count(*) from application_stage_evaluators e where e.application_id = :a and e.stage_id = :s
                  and not exists (select 1 from application_evaluations v where v.application_id = e.application_id
                                  and v.stage_id = e.stage_id and v.evaluator_id = e.user_id and v.status = 'SUBMITTED')""",
                params, Integer.class);
        Decision decision = RecruitmentScoring.decide(PassRule.valueOf(stage.passRule()), stage.passScore(),
                stage.minEvaluations(), pending == null ? 0 : pending, sheets);
        jdbc.update("update application_stage_results set score = :score where application_id = :a and stage_id = :s",
                params.addValue("score", decision.averageScore()));
        if (decision.outcome() == Outcome.UNDECIDED) {
            boolean waitingForDecision = (pending == null || pending == 0) && sheets.size() >= stage.minEvaluations();
            if (waitingForDecision)
                workflow.markStageInProgress(app.id(), stage.id(), StageStatus.AWAITING_EVALUATION, actor,
                        "Đủ phiếu, chờ người phụ trách kết luận vòng \"" + stage.name() + "\"");
            if (stage.id().equals(app.currentStageId()) && !app.status().closed())
                workflow.setStatus(app, ApplicationStatus.EVALUATION_PENDING, actor, null, null);
            return;
        }
        workflow.decideStage(app, stageRow, decision.outcome() == Outcome.PASSED ? StageStatus.PASSED : StageStatus.FAILED,
                decision.averageScore(), actor, null, true);
    }

    static Recommendation derivedRecommendation(Stage stage, BigDecimal total, boolean criticalFailed) {
        if (criticalFailed) return Recommendation.FAIL;
        if (total == null || stage.passScore() == null) return null;
        return total.compareTo(stage.passScore()) >= 0 ? Recommendation.PASS : Recommendation.FAIL;
    }

    private static String label(Recommendation r) {
        return switch (r) {
            case PASS -> "Đạt";
            case FAIL -> "Không đạt";
            case CONSIDER -> "Cân nhắc";
        };
    }

    private Stage stageOf(UUID positionId, UUID stageId) {
        return pipelines.stages(positionId).stream().filter(s -> s.id().equals(stageId)).findFirst()
                .orElseThrow(() -> BusinessException.notFound("Vòng tuyển dụng không thuộc vị trí này"));
    }

    private List<Evaluation> evaluations(UUID applicationId, UUID stageId) {
        MapSqlParameterSource params = new MapSqlParameterSource("a", applicationId).addValue("s", stageId);
        Map<UUID, List<ScoreView>> scores = new HashMap<>();
        jdbc.query("""
                select sc.* from application_evaluation_scores sc join application_evaluations e on e.id = sc.evaluation_id
                where e.application_id = :a and e.stage_id = :s""", params, rs -> {
            scores.computeIfAbsent(rs.getObject("evaluation_id", UUID.class), k -> new ArrayList<>()).add(new ScoreView(
                    rs.getObject("criterion_id", UUID.class), rs.getBigDecimal("score"), (Boolean) rs.getObject("passed"),
                    rs.getString("comment")));
        });
        return jdbc.query("""
                select e.*, u.full_name from application_evaluations e join users u on u.id = e.evaluator_id
                where e.application_id = :a and e.stage_id = :s order by e.submitted_at nulls last, u.full_name""", params,
                (rs, i) -> new Evaluation(rs.getObject("id", UUID.class), rs.getObject("evaluator_id", UUID.class),
                        rs.getString("full_name"), rs.getString("status"), rs.getBigDecimal("total_score"),
                        rs.getString("recommendation"), rs.getString("comment"), rs.getBoolean("critical_failed"),
                        scores.getOrDefault(rs.getObject("id", UUID.class), List.of()), time(rs, "submitted_at"),
                        time(rs, "updated_at")));
    }
}
