package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.helper.mail.MailRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EmailLog;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.EmailTemplate;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.RenderedEmail;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Email cho ứng viên. Nội dung lấy từ mẫu chỉnh được ({@code recruitment_email_templates}, placeholder {{tên}}), ghi
 * nhật ký trước rồi gửi sau khi commit trên luồng nền (SMTP chậm / lỗi không chặn nghiệp vụ); kết quả gửi cập nhật lại
 * nhật ký (SENT / FAILED). Email tự động chỉ gửi khi mẫu đang bật và ứng viên có email.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class RecruitmentMailer {
    public static final String APPLICATION_RECEIVED = "APPLICATION_RECEIVED";
    public static final String INTERVIEW_INVITATION = "INTERVIEW_INVITATION";
    public static final String INTERVIEW_RESCHEDULED = "INTERVIEW_RESCHEDULED";
    public static final String INTERVIEW_CANCELLED = "INTERVIEW_CANCELLED";
    public static final String OFFER_LETTER = "OFFER_LETTER";
    public static final String REJECTION = "REJECTION";
    public static final String ONBOARDING = "ONBOARDING";

    /** Placeholder có thể dùng trong mẫu (hiện ở màn hình sửa mẫu). */
    public static final List<String> PLACEHOLDERS = List.of("candidateName", "positionTitle", "applicationCode", "workplace",
            "campaignName", "interviewTitle", "interviewTime", "interviewMode", "interviewPlace", "jobTitle", "salary",
            "startDate", "responseDeadline", "offerNote", "reason", "employeeCode", "username");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z]+)\\s*}}");

    private final NamedParameterJdbcTemplate jdbc;
    private final MailHelper mail;
    private final TaskExecutor delivery;
    private final ApplicationWorkflow workflow;

    public RecruitmentMailer(NamedParameterJdbcTemplate jdbc, MailHelper mail, ApplicationWorkflow workflow,
                             @Qualifier(RecruitmentMailConfig.EXECUTOR) TaskExecutor delivery) {
        this.jdbc = jdbc;
        this.mail = mail;
        this.workflow = workflow;
        this.delivery = delivery;
    }

    // ------------------------------------------------------------------ gửi

    record Context(UUID candidateId, String email, Map<String, String> vars) {
    }

    /** Biến chung của một hồ sơ: tên ứng viên, vị trí, nơi làm việc… */
    Context context(UUID applicationId) {
        List<Context> rows = jdbc.query("""
                select a.candidate_id, cd.email, cd.full_name, p.title, a.code, c.name as campaign_name,
                       coalesce(p.work_location, st.name, 'Lunaria') as workplace
                from job_applications a join candidates cd on cd.id = a.candidate_id
                join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id
                left join stores st on st.id = c.store_id where a.id = :id""",
                new MapSqlParameterSource("id", applicationId), (rs, i) -> {
                    Map<String, String> vars = new HashMap<>();
                    vars.put("candidateName", rs.getString("full_name"));
                    vars.put("positionTitle", rs.getString("title"));
                    vars.put("applicationCode", rs.getString("code"));
                    vars.put("campaignName", rs.getString("campaign_name"));
                    vars.put("workplace", rs.getString("workplace"));
                    return new Context(rs.getObject("candidate_id", UUID.class), rs.getString("email"), vars);
                });
        if (rows.isEmpty()) throw BusinessException.notFound("Hồ sơ ứng tuyển không tồn tại");
        return rows.getFirst();
    }

    /**
     * Email tự động theo mẫu. Bỏ qua (trả về false) khi mẫu tắt hoặc ứng viên không có email.
     *
     * @param actor null = hệ thống
     */
    @Transactional
    public boolean sendTemplate(UUID applicationId, String templateCode, Map<String, String> extra, UUID actor) {
        List<String[]> template = jdbc.query("select subject, body from recruitment_email_templates where code = :c and enabled",
                new MapSqlParameterSource("c", templateCode), (rs, i) -> new String[]{rs.getString("subject"), rs.getString("body")});
        if (template.isEmpty()) return false;
        Context ctx = context(applicationId);
        if (!StringUtils.hasText(ctx.email())) {
            workflow.history(applicationId, "EMAIL", null, null, "SKIPPED", "Không gửi email \"" + templateName(templateCode)
                    + "\": ứng viên chưa có email", null, actor);
            return false;
        }
        Map<String, String> vars = new HashMap<>(ctx.vars());
        if (extra != null) vars.putAll(extra);
        queue(applicationId, ctx, templateCode, render(template.getFirst()[0], vars), render(template.getFirst()[1], vars), actor);
        return true;
    }

    /** Email soạn tay (có thể dùng placeholder). */
    @Transactional
    public EmailLog sendManual(UUID applicationId, String templateCode, String subject, String body, UUID actor) {
        Context ctx = context(applicationId);
        if (!StringUtils.hasText(ctx.email())) throw BusinessException.badRequest("Ứng viên chưa có email");
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(body)) throw BusinessException.badRequest("Vui lòng nhập tiêu đề và nội dung");
        UUID id = queue(applicationId, ctx, StringUtils.hasText(templateCode) ? templateCode.trim() : null,
                render(subject, ctx.vars()), render(body, ctx.vars()), actor);
        return logs(applicationId).stream().filter(l -> l.id().equals(id)).findFirst().orElseThrow();
    }

    /** Mẫu đã điền biến của hồ sơ, để soạn email tay. */
    public RenderedEmail preview(UUID applicationId, String templateCode) {
        Context ctx = context(applicationId);
        List<RenderedEmail> rows = jdbc.query("select subject, body from recruitment_email_templates where code = :c",
                new MapSqlParameterSource("c", templateCode), (rs, i) -> new RenderedEmail(ctx.email(),
                        render(rs.getString("subject"), ctx.vars()), render(rs.getString("body"), ctx.vars())));
        if (rows.isEmpty()) throw BusinessException.notFound("Mẫu email không tồn tại");
        return rows.getFirst();
    }

    private UUID queue(UUID applicationId, Context ctx, String templateCode, String subject, String body, UUID actor) {
        String safeSubject = ApplicationWorkflow.cut(subject.replaceAll("[\\r\\n]+", " ").trim(), 300);
        UUID id = jdbc.queryForObject("""
                insert into recruitment_emails (application_id, candidate_id, template_code, to_email, subject, body, status, sent_by)
                values (:a, :c, :t, :to, :subject, :body, 'QUEUED', :actor) returning id""",
                new MapSqlParameterSource("a", applicationId).addValue("c", ctx.candidateId()).addValue("t", templateCode)
                        .addValue("to", ctx.email()).addValue("subject", safeSubject).addValue("body", body)
                        .addValue("actor", actor), UUID.class);
        workflow.history(applicationId, "EMAIL", null, null, "QUEUED", "Gửi email \"" + safeSubject + "\" tới " + ctx.email(), null, actor);
        Runnable send = () -> delivery.execute(() -> deliver(id, ctx.email(), safeSubject, body));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
        return id;
    }

    /** Gửi thật và ghi kết quả (ngoài transaction nghiệp vụ). */
    void deliver(UUID logId, String to, String subject, String body) {
        try {
            mail.sendTemplateMail(new MailRequest(List.of(to), subject, body, null), "mail/base",
                    Map.of("title", subject, "content", toHtml(body)));
            jdbc.update("update recruitment_emails set status = 'SENT', sent_at = now() at time zone 'UTC', error = null where id = :id",
                    new MapSqlParameterSource("id", logId));
        } catch (RuntimeException e) {
            log.warn("Recruitment email {} to {} failed", logId, to, e);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            jdbc.update("update recruitment_emails set status = 'FAILED', error = :error where id = :id",
                    new MapSqlParameterSource("id", logId).addValue("error", ApplicationWorkflow.cut(reason, 500)));
        }
    }

    /** Gửi lại email lỗi. */
    @Transactional
    public EmailLog retry(UUID applicationId, UUID emailId) {
        EmailLog found = logs(applicationId).stream().filter(l -> l.id().equals(emailId)).findFirst()
                .orElseThrow(() -> BusinessException.notFound("Email không tồn tại"));
        if (!"FAILED".equals(found.status())) throw BusinessException.invalidState("Chỉ gửi lại được email lỗi");
        jdbc.update("update recruitment_emails set status = 'QUEUED', error = null where id = :id", new MapSqlParameterSource("id", emailId));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delivery.execute(() -> deliver(emailId, found.toEmail(), found.subject(), found.body()));
            }
        });
        return new EmailLog(found.id(), found.templateCode(), found.toEmail(), found.subject(), found.body(), "QUEUED", null,
                found.sentByName(), found.createdAt(), null);
    }

    public List<EmailLog> logs(UUID applicationId) {
        return jdbc.query("""
                select e.*, u.full_name as sent_by_name from recruitment_emails e left join users u on u.id = e.sent_by
                where e.application_id = :a order by e.created_at desc""", new MapSqlParameterSource("a", applicationId),
                (rs, i) -> new EmailLog(rs.getObject("id", UUID.class), rs.getString("template_code"), rs.getString("to_email"),
                        rs.getString("subject"), rs.getString("body"), rs.getString("status"), rs.getString("error"),
                        rs.getString("sent_by_name"), ApplicationWorkflow.time(rs, "created_at"), ApplicationWorkflow.time(rs, "sent_at")));
    }

    /**
     * Email còn "Đang gửi" quá {@code minutes} phút (ứng dụng khởi động lại trước khi gửi xong...) được gửi lại.
     * Gọi từ {@link RecruitmentMailRetryScheduler}.
     */
    public int resendStale(int minutes) {
        record Stale(UUID id, String to, String subject, String body) {
        }
        List<Stale> stale = jdbc.query("""
                select id, to_email, subject, body from recruitment_emails
                where status = 'QUEUED' and created_at < (now() at time zone 'UTC') - make_interval(mins => :m)
                order by created_at limit 50""", new MapSqlParameterSource("m", minutes),
                (rs, i) -> new Stale(rs.getObject("id", UUID.class), rs.getString("to_email"), rs.getString("subject"), rs.getString("body")));
        stale.forEach(s -> deliver(s.id(), s.to(), s.subject(), s.body()));
        return stale.size();
    }

    // ------------------------------------------------------------------ mẫu email

    public List<EmailTemplate> templates(Actor actor) {
        return jdbc.query("""
                select t.*, u.full_name as updated_by_name from recruitment_email_templates t
                left join users u on u.id = t.updated_by order by t.code""", new MapSqlParameterSource(),
                (rs, i) -> new EmailTemplate(rs.getString("code"), rs.getString("name"), rs.getString("subject"),
                        rs.getString("body"), rs.getBoolean("enabled"), rs.getString("updated_by_name"),
                        ApplicationWorkflow.time(rs, "updated_at"), actor.globalAdmin(), PLACEHOLDERS));
    }

    /** Mẫu dùng chung toàn chuỗi nên chỉ HR Admin cấp chuỗi được sửa. */
    @Transactional
    public EmailTemplate updateTemplate(Actor actor, String code, String subject, String body, boolean enabled) {
        if (!actor.globalAdmin()) throw BusinessException.forbidden("Chỉ HR Admin cấp chuỗi được sửa mẫu email");
        int updated = jdbc.update("""
                update recruitment_email_templates set subject = :subject, body = :body, enabled = :enabled,
                    updated_by = :actor, updated_at = now() at time zone 'UTC' where code = :code""",
                new MapSqlParameterSource("code", code).addValue("subject", subject.trim()).addValue("body", body.trim())
                        .addValue("enabled", enabled).addValue("actor", actor.userId()));
        if (updated == 0) throw BusinessException.notFound("Mẫu email không tồn tại");
        return templates(actor).stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ tiện ích

    /** Thay {{biến}}; biến không có giá trị → rỗng; gộp các dòng trống liên tiếp. */
    static String render(String text, Map<String, String> vars) {
        Matcher m = PLACEHOLDER.matcher(text == null ? "" : text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = vars.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value));
        }
        m.appendTail(out);
        return out.toString().replaceAll("(?m)^[ \\t]+$", "").replaceAll("\\n{3,}", "\n\n").trim();
    }

    static String toHtml(String text) {
        return HtmlUtils.htmlEscape(text, "UTF-8").replace("\n", "<br>");
    }

    private String templateName(String code) {
        List<String> names = jdbc.queryForList("select name from recruitment_email_templates where code = :c",
                new MapSqlParameterSource("c", code), String.class);
        return names.isEmpty() ? code : names.getFirst();
    }
}
