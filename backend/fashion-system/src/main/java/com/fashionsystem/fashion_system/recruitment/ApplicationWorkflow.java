package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Luồng của một hồ sơ ứng tuyển: đổi trạng thái tổng thể, vào / ra vòng, kết luận vòng, ghi lịch sử. Mọi service
 * nghiệp vụ (đánh giá, phỏng vấn, offer) đi qua đây để trạng thái và lịch sử luôn nhất quán. Gọi trong transaction.
 *
 * <p>Vào vòng: SCREENING → Chờ sàng lọc; INTERVIEW → Chờ phỏng vấn; OFFER → Chờ Offer; vòng khác → Đang xử lý.
 * Kết luận vòng: Đạt → "Đạt yêu cầu" (vòng sàng lọc) hoặc "Đạt vòng tuyển dụng"; vòng bật tự chuyển thì sang luôn vòng
 * kế tiếp. Không đạt → "Không đạt vòng tuyển dụng" (HR xác nhận loại bằng trạng thái "Không đạt"). Qua vòng cuối → "Đạt
 * yêu cầu", sẵn sàng tạo offer.
 */
@Component
public class ApplicationWorkflow {
    private final NamedParameterJdbcTemplate jdbc;

    public ApplicationWorkflow(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record AppState(UUID id, UUID positionId, ApplicationStatus status, UUID currentStageId) {
    }

    record StageRow(UUID id, String name, int sortOrder, String stageType, String passRule, boolean autoAdvance) {
    }

    // ------------------------------------------------------------------ đọc

    AppState lock(UUID applicationId) {
        List<AppState> rows = jdbc.query("select id, position_id, status, current_stage_id from job_applications where id = :id for update",
                new MapSqlParameterSource("id", applicationId), (rs, i) -> new AppState(rs.getObject("id", UUID.class),
                        rs.getObject("position_id", UUID.class), ApplicationStatus.valueOf(rs.getString("status")),
                        rs.getObject("current_stage_id", UUID.class)));
        if (rows.isEmpty()) throw BusinessException.notFound("Hồ sơ ứng tuyển không tồn tại");
        return rows.getFirst();
    }

    List<StageRow> stages(UUID positionId) {
        return jdbc.query("select id, name, sort_order, stage_type, pass_rule, auto_advance from recruitment_stages where position_id = :p order by sort_order, created_at",
                new MapSqlParameterSource("p", positionId), (rs, i) -> new StageRow(rs.getObject("id", UUID.class),
                        rs.getString("name"), rs.getInt("sort_order"), rs.getString("stage_type"), rs.getString("pass_rule"),
                        rs.getBoolean("auto_advance")));
    }

    StageRow stage(UUID positionId, UUID stageId) {
        return stages(positionId).stream().filter(s -> s.id().equals(stageId)).findFirst()
                .orElseThrow(() -> BusinessException.notFound("Vòng tuyển dụng không thuộc vị trí này"));
    }

    StageStatus stageStatus(UUID applicationId, UUID stageId) {
        List<String> rows = jdbc.queryForList("select status from application_stage_results where application_id = :a and stage_id = :s",
                new MapSqlParameterSource("a", applicationId).addValue("s", stageId), String.class);
        return rows.isEmpty() ? StageStatus.PENDING : StageStatus.valueOf(rows.getFirst());
    }

    static void requireOpen(AppState app) {
        if (app.status().closed())
            throw BusinessException.invalidState("Hồ sơ đã kết thúc (" + app.status().label() + ")");
    }

    // ------------------------------------------------------------------ ghi

    /** Đổi trạng thái tổng thể (không kiểm tra bảng chuyển; người gọi tự kiểm tra) và ghi lịch sử. */
    void setStatus(AppState app, ApplicationStatus target, UUID actor, String message, String reason) {
        if (app.status() == target) return;
        jdbc.update("""
                update job_applications set status = :status, updated_at = now() at time zone 'UTC',
                       closed_reason = case when :closed then :reason else closed_reason end
                where id = :id""", new MapSqlParameterSource("status", target.name()).addValue("id", app.id())
                .addValue("closed", target.closed()).addValue("reason", trim(reason)));
        history(app.id(), "STATUS", null, app.status().name(), target.name(), message, reason, actor);
    }

    /** Đổi trạng thái thủ công theo bảng chuyển hợp lệ. */
    void changeStatusManually(AppState app, ApplicationStatus target, UUID actor, String reason) {
        if (target.systemOnly())
            throw BusinessException.badRequest("Trạng thái \"" + target.label() + "\" do thao tác offer / tuyển dụng tự cập nhật");
        if (!app.status().canMoveTo(target))
            throw BusinessException.invalidState("Không thể chuyển từ \"" + app.status().label() + "\" sang \"" + target.label() + "\"");
        if ((target == ApplicationStatus.REJECTED || target == ApplicationStatus.WITHDRAWN || app.status().closed())
                && !StringUtils.hasText(reason))
            throw BusinessException.badRequest("Vui lòng nhập lý do");
        setStatus(app, target, actor, null, reason);
    }

    /** Đưa hồ sơ vào vòng và đặt trạng thái tổng thể tương ứng loại vòng. */
    void enterStage(AppState app, StageRow stage, UUID actor, String note) {
        jdbc.update("update job_applications set current_stage_id = :s, updated_at = now() at time zone 'UTC' where id = :id",
                new MapSqlParameterSource("s", stage.id()).addValue("id", app.id()));
        jdbc.update("""
                insert into application_stage_results (application_id, stage_id, status, started_at)
                values (:a, :s, 'PENDING', now() at time zone 'UTC')
                on conflict (application_id, stage_id) do update set status = 'PENDING', score = null, decided_by = null,
                    decided_at = null, decision_note = null, started_at = now() at time zone 'UTC'""",
                new MapSqlParameterSource("a", app.id()).addValue("s", stage.id()));
        history(app.id(), "STAGE", stage.id(), null, StageStatus.PENDING.name(), "Chuyển sang vòng \"" + stage.name() + "\"", note, actor);
        AppState moved = new AppState(app.id(), app.positionId(), app.status(), stage.id());
        setStatus(moved, statusOnEnter(stage.stageType()), actor, null, null);
    }

    static ApplicationStatus statusOnEnter(String stageType) {
        return switch (stageType) {
            case "SCREENING" -> ApplicationStatus.SCREENING_PENDING;
            case "INTERVIEW" -> ApplicationStatus.INTERVIEW_PENDING;
            case "OFFER" -> ApplicationStatus.OFFER_PENDING;
            default -> ApplicationStatus.PROCESSING;
        };
    }

    /** Vòng đầu tiên khi hồ sơ mới tạo; vị trí chưa có vòng thì hồ sơ nằm ở "Mới tiếp nhận". */
    void start(UUID applicationId, UUID actor) {
        AppState app = lock(applicationId);
        List<StageRow> stages = stages(app.positionId());
        history(applicationId, "STATUS", null, null, ApplicationStatus.NEW.name(), "Tiếp nhận hồ sơ", null, actor);
        if (!stages.isEmpty()) enterStage(app, stages.getFirst(), actor, null);
    }

    /** Đánh dấu vòng đang thực hiện (có người bắt đầu chấm / có lịch phỏng vấn). */
    void markStageInProgress(UUID applicationId, UUID stageId, StageStatus status, UUID actor, String message) {
        int updated = jdbc.update("""
                update application_stage_results set status = :status
                where application_id = :a and stage_id = :s and status in ('PENDING', 'IN_PROGRESS', 'AWAITING_EVALUATION')
                  and status <> :status""",
                new MapSqlParameterSource("a", applicationId).addValue("s", stageId).addValue("status", status.name()));
        if (updated > 0 && message != null) history(applicationId, "STAGE", stageId, null, status.name(), message, null, actor);
    }

    /**
     * Kết luận vòng (tự động từ phiếu đánh giá hoặc thủ công). Vòng hiện tại Đạt và bật tự chuyển → sang vòng kế.
     */
    void decideStage(AppState app, StageRow stage, StageStatus outcome, BigDecimal score, UUID actor, String note,
                     boolean automatic) {
        if (!outcome.decided()) throw new IllegalArgumentException("outcome must be decided");
        jdbc.update("""
                insert into application_stage_results (application_id, stage_id, status, score, decided_by, decided_at, decision_note, started_at)
                values (:a, :s, :status, :score, :by, now() at time zone 'UTC', :note, now() at time zone 'UTC')
                on conflict (application_id, stage_id) do update set status = :status, score = coalesce(:score, application_stage_results.score),
                    decided_by = :by, decided_at = now() at time zone 'UTC', decision_note = :note""",
                new MapSqlParameterSource("a", app.id()).addValue("s", stage.id()).addValue("status", outcome.name())
                        .addValue("score", score).addValue("by", automatic ? null : actor).addValue("note", trim(note)));
        String scoreText = score == null ? "" : " (điểm " + score.stripTrailingZeros().toPlainString() + ")";
        history(app.id(), "STAGE", stage.id(), null, outcome.name(),
                (automatic ? "Hệ thống kết luận" : "Kết luận") + " vòng \"" + stage.name() + "\": " + outcome.label() + scoreText,
                note, automatic ? null : actor);
        if (!stage.id().equals(app.currentStageId()) || app.status().closed()) return;
        if (outcome == StageStatus.FAILED) {
            setStatus(app, ApplicationStatus.ROUND_FAILED, actor, null, null);
        } else if (outcome == StageStatus.PASSED || outcome == StageStatus.SKIPPED) {
            if (outcome == StageStatus.PASSED && stage.autoAdvance()) {
                advance(app, actor, null);
            } else {
                setStatus(app, "SCREENING".equals(stage.stageType()) ? ApplicationStatus.QUALIFIED : ApplicationStatus.ROUND_PASSED,
                        actor, null, null);
            }
        }
    }

    /** Sang vòng kế tiếp; hết vòng → "Đạt yêu cầu". Vòng hiện tại phải Đạt hoặc Bỏ qua. */
    void advance(AppState app, UUID actor, String note) {
        requireOpen(app);
        List<StageRow> stages = stages(app.positionId());
        if (app.currentStageId() != null) {
            StageStatus current = stageStatus(app.id(), app.currentStageId());
            if (current != StageStatus.PASSED && current != StageStatus.SKIPPED)
                throw BusinessException.invalidState("Vòng hiện tại chưa có kết quả Đạt");
        }
        int index = indexOf(stages, app.currentStageId());
        if (index + 1 < stages.size()) {
            enterStage(app, stages.get(index + 1), actor, note);
        } else {
            setStatus(app, ApplicationStatus.QUALIFIED, actor, "Đã qua toàn bộ các vòng", note);
        }
    }

    /** Chuyển tới một vòng bất kỳ (bỏ qua các vòng ở giữa nếu đi tới). */
    void moveTo(AppState app, StageRow target, UUID actor, String note) {
        requireOpen(app);
        if (target.id().equals(app.currentStageId())) throw BusinessException.badRequest("Hồ sơ đang ở vòng này");
        List<StageRow> stages = stages(app.positionId());
        int from = indexOf(stages, app.currentStageId());
        int to = indexOf(stages, target.id());
        for (int i = Math.max(from, 0); i < to; i++) {
            StageRow skipped = stages.get(i);
            if (!stageStatus(app.id(), skipped.id()).decided()) {
                jdbc.update("""
                        insert into application_stage_results (application_id, stage_id, status, decided_by, decided_at, decision_note)
                        values (:a, :s, 'SKIPPED', :by, now() at time zone 'UTC', :note)
                        on conflict (application_id, stage_id) do update set status = 'SKIPPED', decided_by = :by,
                            decided_at = now() at time zone 'UTC', decision_note = :note""",
                        new MapSqlParameterSource("a", app.id()).addValue("s", skipped.id()).addValue("by", actor)
                                .addValue("note", trim(note)));
                history(app.id(), "STAGE", skipped.id(), null, StageStatus.SKIPPED.name(), "Bỏ qua vòng \"" + skipped.name() + "\"", note, actor);
            }
        }
        enterStage(app, target, actor, note);
    }

    static int indexOf(List<StageRow> stages, UUID stageId) {
        for (int i = 0; i < stages.size(); i++) if (stages.get(i).id().equals(stageId)) return i;
        return -1;
    }

    void history(UUID applicationId, String kind, UUID stageId, String from, String to, String message, String reason, UUID actor) {
        jdbc.update("""
                insert into application_history (application_id, kind, stage_id, from_status, to_status, message, reason, actor_id)
                values (:a, :kind, :stage, :from, :to, :message, :reason, :actor)""",
                new MapSqlParameterSource("a", applicationId).addValue("kind", kind).addValue("stage", stageId)
                        .addValue("from", from).addValue("to", to).addValue("message", cut(message, 1000))
                        .addValue("reason", cut(trim(reason), 1000)).addValue("actor", actor));
    }

    // ------------------------------------------------------------------ tiện ích dùng chung

    String nextCode(String prefix, String sequence) {
        Long value = jdbc.queryForObject("select nextval('" + sequence + "')", new MapSqlParameterSource(), Long.class);
        return prefix + String.format(Locale.ROOT, "%05d", value);
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        if (!StringUtils.hasText(value)) throw BusinessException.badRequest(field + " không được để trống");
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest(field + " không hợp lệ: " + value);
        }
    }

    static String requireOneOf(String value, String field, String... allowed) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList(allowed).contains(normalized)) throw BusinessException.badRequest(field + " không hợp lệ: " + value);
        return normalized;
    }

    static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        return DbTimes.fromDb(rs.getTimestamp(column));
    }

    static List<String> textArray(ResultSet rs, String column) throws SQLException {
        java.sql.Array array = rs.getArray(column);
        if (array == null) return List.of();
        return Arrays.asList((String[]) array.getArray());
    }

    static String[] toArray(List<String> values) {
        if (values == null) return new String[0];
        return values.stream().filter(StringUtils::hasText).map(String::trim).distinct().toArray(String[]::new);
    }
}
