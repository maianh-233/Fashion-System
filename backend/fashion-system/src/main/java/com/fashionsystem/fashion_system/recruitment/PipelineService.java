package com.fashionsystem.fashion_system.recruitment;

import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.trim;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Criterion;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CriterionRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Stage;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.StageRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Template;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.TemplateRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Quy trình tuyển dụng của từng vị trí: danh sách vòng (thêm / sửa / xóa / sắp xếp) và bộ tiêu chí của từng vòng;
 * mẫu quy trình để tái sử dụng. Không có vòng hay tiêu chí nào được code cứng.
 */
@Service
@Transactional(readOnly = true)
public class PipelineService {
    static final String[] STAGE_TYPES = {"SCREENING", "TEST", "INTERVIEW", "OFFER", "OTHER"};
    static final String[] SCORING_MODES = {"SCORE", "PASS_FAIL"};
    static final String[] PASS_RULES = {"AVERAGE_SCORE", "ALL_PASS", "MAJORITY_PASS", "MANUAL"};
    static final String[] IMPORTANCE = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
    private static final int MAX_STAGES = 15;
    private static final int MAX_CRITERIA = 30;

    private final NamedParameterJdbcTemplate jdbc;
    private final RecruitmentAccess access;
    private final ObjectMapper json;

    public PipelineService(NamedParameterJdbcTemplate jdbc, RecruitmentAccess access, ObjectMapper json) {
        this.jdbc = jdbc;
        this.access = access;
        this.json = json;
    }

    // ------------------------------------------------------------------ đọc

    public List<Stage> stages(UUID positionId) {
        MapSqlParameterSource params = new MapSqlParameterSource("p", positionId);
        Map<UUID, List<Criterion>> criteria = new LinkedHashMap<>();
        jdbc.query("""
                select c.* from recruitment_stage_criteria c join recruitment_stages s on s.id = c.stage_id
                where s.position_id = :p order by c.sort_order""", params, rs -> {
            criteria.computeIfAbsent(rs.getObject("stage_id", UUID.class), k -> new ArrayList<>()).add(new Criterion(
                    rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("description"),
                    rs.getBigDecimal("max_score"), rs.getBigDecimal("weight"), rs.getString("importance"), rs.getInt("sort_order")));
        });
        return jdbc.query("""
                select s.*, u.full_name as owner_name from recruitment_stages s left join users u on u.id = s.owner_id
                where s.position_id = :p order by s.sort_order, s.created_at""", params, (rs, i) -> new Stage(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("description"), rs.getInt("sort_order"),
                rs.getString("stage_type"), rs.getObject("owner_id", UUID.class), rs.getString("owner_name"),
                rs.getString("scoring_mode"), rs.getBigDecimal("scale_max"), rs.getBigDecimal("pass_score"),
                rs.getString("pass_rule"), rs.getInt("min_evaluations"), rs.getBoolean("auto_advance"),
                criteria.getOrDefault(rs.getObject("id", UUID.class), List.of())));
    }

    // ------------------------------------------------------------------ lưu quy trình của vị trí

    /**
     * Lưu toàn bộ quy trình theo thứ tự gửi lên: vòng có id thì cập nhật, không id thì thêm, vòng cũ không còn trong
     * danh sách thì xóa (chỉ khi chưa có hồ sơ nào đi qua). Tiêu chí xử lý tương tự trong từng vòng.
     */
    @Transactional
    public List<Stage> savePipeline(Actor actor, UUID positionId, List<StageRequest> requested) {
        access.requireManagePosition(actor, positionId);
        validate(requested);
        List<UUID> existing = jdbc.queryForList("select id from recruitment_stages where position_id = :p",
                new MapSqlParameterSource("p", positionId), UUID.class);
        Set<UUID> kept = new HashSet<>();
        for (StageRequest r : requested) {
            if (r.id() != null) {
                if (!existing.contains(r.id())) throw BusinessException.badRequest("Vòng \"" + r.name() + "\" không thuộc vị trí này");
                if (!kept.add(r.id())) throw BusinessException.badRequest("Vòng \"" + r.name() + "\" bị lặp");
            }
        }
        for (UUID removed : existing) {
            if (kept.contains(removed)) continue;
            Boolean used = jdbc.queryForObject("""
                    select exists (select 1 from application_stage_results where stage_id = :s)
                        or exists (select 1 from job_applications where current_stage_id = :s)
                        or exists (select 1 from application_evaluations where stage_id = :s)""",
                    new MapSqlParameterSource("s", removed), Boolean.class);
            if (Boolean.TRUE.equals(used)) {
                String name = jdbc.queryForObject("select name from recruitment_stages where id = :s", new MapSqlParameterSource("s", removed), String.class);
                throw BusinessException.invalidState("Vòng \"" + name + "\" đã có hồ sơ đi qua, không thể xóa");
            }
            jdbc.update("delete from recruitment_stages where id = :s", new MapSqlParameterSource("s", removed));
        }
        int order = 1;
        for (StageRequest r : requested) {
            UUID stageId;
            if (r.ownerId() != null) access.requireAssignable(r.ownerId(), access.storeOfPosition(positionId), "Người phụ trách vòng");
            MapSqlParameterSource params = stageParams(r).addValue("p", positionId).addValue("order", order++);
            if (r.id() == null) {
                stageId = jdbc.queryForObject("""
                        insert into recruitment_stages (position_id, name, description, sort_order, stage_type, owner_id, scoring_mode,
                            scale_max, pass_score, pass_rule, min_evaluations, auto_advance)
                        values (:p, :name, :description, :order, :type, :owner, :mode, :scale, :pass, :rule, :minEval, :auto)
                        returning id""", params, UUID.class);
            } else {
                stageId = r.id();
                jdbc.update("""
                        update recruitment_stages set name = :name, description = :description, sort_order = :order,
                            stage_type = :type, owner_id = :owner, scoring_mode = :mode, scale_max = :scale, pass_score = :pass,
                            pass_rule = :rule, min_evaluations = :minEval, auto_advance = :auto, updated_at = now() at time zone 'UTC'
                        where id = :id""", params.addValue("id", stageId));
            }
            saveCriteria(stageId, r.name(), r.criteria() == null ? List.of() : r.criteria());
        }
        return stages(positionId);
    }

    private void saveCriteria(UUID stageId, String stageName, List<CriterionRequest> requested) {
        List<UUID> existing = jdbc.queryForList("select id from recruitment_stage_criteria where stage_id = :s",
                new MapSqlParameterSource("s", stageId), UUID.class);
        Set<UUID> kept = new HashSet<>();
        for (CriterionRequest c : requested) {
            if (c.id() != null && (!existing.contains(c.id()) || !kept.add(c.id())))
                throw BusinessException.badRequest("Tiêu chí \"" + c.name() + "\" không thuộc vòng \"" + stageName + "\"");
        }
        for (UUID removed : existing) {
            if (kept.contains(removed)) continue;
            Boolean scored = jdbc.queryForObject("""
                    select exists (select 1 from application_evaluation_scores sc join application_evaluations e on e.id = sc.evaluation_id
                                   where sc.criterion_id = :c and e.status = 'SUBMITTED')""",
                    new MapSqlParameterSource("c", removed), Boolean.class);
            if (Boolean.TRUE.equals(scored))
                throw BusinessException.invalidState("Một tiêu chí của vòng \"" + stageName + "\" đã được chấm, không thể xóa");
            jdbc.update("delete from recruitment_stage_criteria where id = :c", new MapSqlParameterSource("c", removed));
        }
        int order = 1;
        for (CriterionRequest c : requested) {
            MapSqlParameterSource params = new MapSqlParameterSource("s", stageId).addValue("name", c.name().trim())
                    .addValue("description", trim(c.description())).addValue("max", c.maxScore()).addValue("weight", c.weight())
                    .addValue("importance", ApplicationWorkflow.requireOneOf(c.importance(), "Mức độ quan trọng", IMPORTANCE))
                    .addValue("order", order++);
            if (c.id() == null) {
                jdbc.update("""
                        insert into recruitment_stage_criteria (stage_id, name, description, max_score, weight, importance, sort_order)
                        values (:s, :name, :description, :max, :weight, :importance, :order)""", params);
            } else {
                jdbc.update("""
                        update recruitment_stage_criteria set name = :name, description = :description, max_score = :max,
                            weight = :weight, importance = :importance, sort_order = :order where id = :id""", params.addValue("id", c.id()));
            }
        }
    }

    private MapSqlParameterSource stageParams(StageRequest r) {
        if (r.ownerId() != null && !Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from users where id = :id and deleted_at is null)",
                new MapSqlParameterSource("id", r.ownerId()), Boolean.class)))
            throw BusinessException.notFound("Người phụ trách vòng \"" + r.name() + "\" không tồn tại");
        return new MapSqlParameterSource("name", r.name().trim()).addValue("description", trim(r.description()))
                .addValue("type", ApplicationWorkflow.requireOneOf(r.stageType(), "Loại vòng", STAGE_TYPES))
                .addValue("owner", r.ownerId())
                .addValue("mode", ApplicationWorkflow.requireOneOf(r.scoringMode(), "Cách chấm", SCORING_MODES))
                .addValue("scale", r.scaleMax()).addValue("pass", r.passScore())
                .addValue("rule", ApplicationWorkflow.requireOneOf(r.passRule(), "Điều kiện đạt", PASS_RULES))
                .addValue("minEval", r.minEvaluations() == null ? 1 : r.minEvaluations())
                .addValue("auto", Boolean.TRUE.equals(r.autoAdvance()));
    }

    /** Kiểm tra cấu hình vòng (dùng cho cả quy trình vị trí và mẫu). */
    static void validate(List<StageRequest> stages) {
        if (stages.size() > MAX_STAGES) throw BusinessException.badRequest("Tối đa " + MAX_STAGES + " vòng");
        Set<String> names = new HashSet<>();
        for (StageRequest s : stages) {
            if (!names.add(s.name().trim().toLowerCase())) throw BusinessException.badRequest("Tên vòng \"" + s.name() + "\" bị trùng");
            String rule = ApplicationWorkflow.requireOneOf(s.passRule(), "Điều kiện đạt", PASS_RULES);
            ApplicationWorkflow.requireOneOf(s.stageType(), "Loại vòng", STAGE_TYPES);
            String mode = ApplicationWorkflow.requireOneOf(s.scoringMode(), "Cách chấm", SCORING_MODES);
            List<CriterionRequest> criteria = s.criteria() == null ? List.of() : s.criteria();
            if (criteria.size() > MAX_CRITERIA) throw BusinessException.badRequest("Mỗi vòng tối đa " + MAX_CRITERIA + " tiêu chí");
            if (s.passScore() != null && s.passScore().compareTo(s.scaleMax()) > 0)
                throw BusinessException.badRequest("Điểm đạt của vòng \"" + s.name() + "\" vượt thang điểm");
            if ("AVERAGE_SCORE".equals(rule)) {
                if (s.passScore() == null)
                    throw BusinessException.badRequest("Vòng \"" + s.name() + "\" chấm theo điểm trung bình cần điểm đạt tối thiểu");
                if (criteria.isEmpty())
                    throw BusinessException.badRequest("Vòng \"" + s.name() + "\" chấm theo điểm cần ít nhất một tiêu chí");
            }
            if ("PASS_FAIL".equals(mode) && "AVERAGE_SCORE".equals(rule) && criteria.isEmpty())
                throw BusinessException.badRequest("Vòng \"" + s.name() + "\" cần tiêu chí để chấm Đạt/Không đạt");
            Set<String> criterionNames = new HashSet<>();
            for (CriterionRequest c : criteria) {
                if (!criterionNames.add(c.name().trim().toLowerCase()))
                    throw BusinessException.badRequest("Tiêu chí \"" + c.name() + "\" bị trùng trong vòng \"" + s.name() + "\"");
                ApplicationWorkflow.requireOneOf(c.importance(), "Mức độ quan trọng", IMPORTANCE);
            }
        }
    }

    // ------------------------------------------------------------------ mẫu quy trình

    public List<Template> templates(Actor actor) {
        return jdbc.query("""
                select t.*, u.full_name as created_by_name from recruitment_pipeline_templates t
                left join users u on u.id = t.created_by order by t.name""", new MapSqlParameterSource(), (rs, i) -> new Template(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("description"),
                readStages(rs.getString("stages")), rs.getString("created_by_name"), ApplicationWorkflow.time(rs, "created_at"),
                ApplicationWorkflow.time(rs, "updated_at"),
                actor.globalAdmin() || ((actor.manage() || actor.admin()) && actor.userId().equals(rs.getObject("created_by", UUID.class)))));
    }

    @Transactional
    public Template createTemplate(Actor actor, TemplateRequest request) {
        if (!actor.admin() && !actor.manage()) throw BusinessException.forbidden("Bạn không có quyền tạo mẫu quy trình");
        validate(request.stages());
        UUID id = jdbc.queryForObject("""
                insert into recruitment_pipeline_templates (name, description, stages, created_by)
                values (:name, :description, cast(:stages as jsonb), :actor) returning id""",
                new MapSqlParameterSource("name", request.name().trim()).addValue("description", trim(request.description()))
                        .addValue("stages", writeStages(request.stages())).addValue("actor", actor.userId()), UUID.class);
        return template(actor, id);
    }

    @Transactional
    public Template updateTemplate(Actor actor, UUID id, TemplateRequest request) {
        requireEditTemplate(actor, id);
        validate(request.stages());
        jdbc.update("""
                update recruitment_pipeline_templates set name = :name, description = :description, stages = cast(:stages as jsonb),
                    updated_at = now() at time zone 'UTC' where id = :id""",
                new MapSqlParameterSource("id", id).addValue("name", request.name().trim())
                        .addValue("description", trim(request.description())).addValue("stages", writeStages(request.stages())));
        return template(actor, id);
    }

    @Transactional
    public void deleteTemplate(Actor actor, UUID id) {
        requireEditTemplate(actor, id);
        jdbc.update("delete from recruitment_pipeline_templates where id = :id", new MapSqlParameterSource("id", id));
    }

    /** Lưu quy trình hiện tại của vị trí thành mẫu. */
    @Transactional
    public Template saveAsTemplate(Actor actor, UUID positionId, String name, String description) {
        access.requireViewPosition(actor, positionId);
        List<StageRequest> stages = stages(positionId).stream().map(PipelineService::withoutIds).toList();
        if (stages.isEmpty()) throw BusinessException.badRequest("Vị trí chưa có vòng nào để lưu thành mẫu");
        return createTemplate(actor, new TemplateRequest(name, description, stages));
    }

    /** Thay quy trình của vị trí bằng mẫu (chỉ khi chưa có hồ sơ nào bắt đầu quy trình). */
    @Transactional
    public List<Stage> applyTemplate(Actor actor, UUID positionId, UUID templateId) {
        access.requireManagePosition(actor, positionId);
        applyTemplateInternal(positionId, templateId);
        return stages(positionId);
    }

    void applyTemplateInternal(UUID positionId, UUID templateId) {
        List<String> found = jdbc.queryForList("select stages::text from recruitment_pipeline_templates where id = :id",
                new MapSqlParameterSource("id", templateId), String.class);
        if (found.isEmpty()) throw BusinessException.notFound("Mẫu quy trình không tồn tại");
        Boolean started = jdbc.queryForObject("""
                select exists (select 1 from application_stage_results r join recruitment_stages s on s.id = r.stage_id
                               where s.position_id = :p)""", new MapSqlParameterSource("p", positionId), Boolean.class);
        if (Boolean.TRUE.equals(started))
            throw BusinessException.invalidState("Vị trí đã có hồ sơ trong quy trình, hãy sửa từng vòng thay vì áp dụng mẫu");
        List<StageRequest> stages = readStages(found.getFirst());
        validate(stages);
        jdbc.update("delete from recruitment_stages where position_id = :p", new MapSqlParameterSource("p", positionId));
        int order = 1;
        for (StageRequest r : stages) {
            UUID stageId = jdbc.queryForObject("""
                    insert into recruitment_stages (position_id, name, description, sort_order, stage_type, owner_id, scoring_mode,
                        scale_max, pass_score, pass_rule, min_evaluations, auto_advance)
                    values (:p, :name, :description, :order, :type, null, :mode, :scale, :pass, :rule, :minEval, :auto)
                    returning id""", stageParams(new StageRequest(null, r.name(), r.description(), r.stageType(), null,
                    r.scoringMode(), r.scaleMax(), r.passScore(), r.passRule(), r.minEvaluations(), r.autoAdvance(), r.criteria()))
                    .addValue("p", positionId).addValue("order", order++), UUID.class);
            saveCriteria(stageId, r.name(), r.criteria() == null ? List.of()
                    : r.criteria().stream().map(c -> new CriterionRequest(null, c.name(), c.description(), c.maxScore(),
                    c.weight(), c.importance())).toList());
        }
    }

    private Template template(Actor actor, UUID id) {
        return templates(actor).stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> BusinessException.notFound("Mẫu quy trình không tồn tại"));
    }

    private void requireEditTemplate(Actor actor, UUID id) {
        List<UUID> owner = jdbc.queryForList("select created_by from recruitment_pipeline_templates where id = :id",
                new MapSqlParameterSource("id", id), UUID.class);
        if (owner.isEmpty()) throw BusinessException.notFound("Mẫu quy trình không tồn tại");
        if (!actor.globalAdmin() && !((actor.manage() || actor.admin()) && actor.userId().equals(owner.getFirst())))
            throw BusinessException.forbidden("Chỉ HR Admin cấp chuỗi hoặc người tạo được sửa mẫu này");
    }

    static StageRequest withoutIds(Stage s) {
        return new StageRequest(null, s.name(), s.description(), s.stageType(), null, s.scoringMode(), s.scaleMax(),
                s.passScore(), s.passRule(), s.minEvaluations(), s.autoAdvance(), s.criteria().stream()
                .map(c -> new CriterionRequest(null, c.name(), c.description(), c.maxScore(), c.weight(), c.importance()))
                .toList());
    }

    private String writeStages(List<StageRequest> stages) {
        return json.writeValueAsString(stages.stream().map(s -> new StageRequest(null, s.name().trim(), trim(s.description()),
                s.stageType().trim().toUpperCase(), null, s.scoringMode().trim().toUpperCase(), s.scaleMax(), s.passScore(),
                s.passRule().trim().toUpperCase(), s.minEvaluations() == null ? 1 : s.minEvaluations(),
                Boolean.TRUE.equals(s.autoAdvance()), s.criteria() == null ? List.of() : s.criteria().stream()
                .map(c -> new CriterionRequest(null, c.name().trim(), trim(c.description()), c.maxScore(), c.weight(),
                        c.importance().trim().toUpperCase())).toList())).toList());
    }

    private List<StageRequest> readStages(String value) {
        return json.readValue(value, new TypeReference<List<StageRequest>>() {
        });
    }
}
