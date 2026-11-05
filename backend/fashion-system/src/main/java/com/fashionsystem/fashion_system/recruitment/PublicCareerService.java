package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CandidateRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PublicApplyResult;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PublicPosition;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Trang tuyển dụng công khai: vị trí đang mở (chiến dịch mở, vị trí mở, công khai, còn hạn) và biểu mẫu nộp hồ sơ. Ứng
 * viên trùng email / số điện thoại được gộp vào hồ sơ ứng viên cũ thay vì tạo bản trùng. Giới hạn số lần nộp theo IP.
 */
@Service
@Transactional(readOnly = true)
public class PublicCareerService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ApplicationWorkflow workflow;
    private final CandidateService candidates;
    private final ApplicationService applications;
    private final CandidateDocumentService documents;
    private final int maxPerWindow;
    private final long windowMillis;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public PublicCareerService(NamedParameterJdbcTemplate jdbc, ApplicationWorkflow workflow, CandidateService candidates,
                               ApplicationService applications, CandidateDocumentService documents,
                               @Value("${recruitment.public-apply.max-per-window:5}") int maxPerWindow,
                               @Value("${recruitment.public-apply.window-minutes:10}") long windowMinutes) {
        this.jdbc = jdbc;
        this.workflow = workflow;
        this.candidates = candidates;
        this.applications = applications;
        this.documents = documents;
        this.maxPerWindow = maxPerWindow;
        this.windowMillis = windowMinutes * 60_000;
    }

    private static final String OPEN = """
            from recruitment_positions p
            join recruitment_campaigns c on c.id = p.campaign_id
            left join stores st on st.id = c.store_id
            left join departments d on d.id = p.department_id
            where p.is_public and p.status = 'OPEN' and c.status = 'OPEN'
              and (p.deadline is null or p.deadline >= :today)
              and (c.end_date is null or c.end_date >= :today)
            """;

    public List<PublicPosition> positions(String keyword, UUID storeId) {
        MapSqlParameterSource params = new MapSqlParameterSource("today", LocalDate.now());
        StringBuilder where = new StringBuilder();
        if (StringUtils.hasText(keyword)) {
            where.append(" and (p.title ilike :kw or p.work_location ilike :kw or st.name ilike :kw)");
            params.addValue("kw", "%" + keyword.trim() + "%");
        }
        if (storeId != null) { where.append(" and c.store_id = :store"); params.addValue("store", storeId); }
        return jdbc.query("select p.*, c.name as campaign_name, st.name as store_name, st.address as store_address, d.name as department_name "
                + OPEN + where + " order by p.created_at desc limit 200", params, (rs, i) -> position(rs));
    }

    public PublicPosition position(String code) {
        List<PublicPosition> rows = jdbc.query("select p.*, c.name as campaign_name, st.name as store_name, st.address as store_address, d.name as department_name "
                + OPEN + " and p.code = :code", new MapSqlParameterSource("today", LocalDate.now()).addValue("code", code),
                (rs, i) -> position(rs));
        if (rows.isEmpty()) throw BusinessException.notFound("Vị trí không tồn tại hoặc đã ngừng nhận hồ sơ");
        return rows.getFirst();
    }

    private static PublicPosition position(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PublicPosition(rs.getString("code"), rs.getString("title"), rs.getString("campaign_name"),
                rs.getString("store_name"), rs.getString("store_address"), rs.getString("department_name"),
                rs.getString("employment_type"), rs.getString("work_location"), rs.getInt("headcount"),
                ApplicationWorkflow.textArray(rs, "skills"), rs.getBigDecimal("experience_min"), rs.getBigDecimal("experience_max"),
                rs.getBoolean("salary_negotiable") ? null : rs.getBigDecimal("salary_min"),
                rs.getBoolean("salary_negotiable") ? null : rs.getBigDecimal("salary_max"), rs.getBoolean("salary_negotiable"),
                rs.getObject("deadline", LocalDate.class), rs.getString("job_description"), rs.getString("requirements"),
                rs.getString("benefits"));
    }

    public record ApplyForm(String fullName, String email, String phone, LocalDate dateOfBirth, String gender,
                            String address, String currentTitle, String currentCompany, java.math.BigDecimal yearsExperience,
                            List<String> skills, String education, String linkedinUrl, String portfolioUrl,
                            String coverNote, String source, Boolean consent) {
    }

    @Transactional
    public PublicApplyResult apply(String code, ApplyForm form, MultipartFile cv, MultipartFile coverLetter,
                                   MultipartFile portfolio, String clientKey) {
        throttle(clientKey);
        if (!Boolean.TRUE.equals(form.consent()))
            throw BusinessException.badRequest("Vui lòng đồng ý cho phép lưu và xử lý hồ sơ");
        if (!StringUtils.hasText(form.fullName()) || form.fullName().trim().length() > 200)
            throw BusinessException.badRequest("Vui lòng nhập họ tên");
        if (!StringUtils.hasText(form.email()) || !form.email().trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw BusinessException.badRequest("Email không hợp lệ");
        if (!StringUtils.hasText(form.phone()) || !form.phone().trim().matches("^\\+?[0-9 .()-]{8,20}$"))
            throw BusinessException.badRequest("Số điện thoại không hợp lệ");
        if (cv == null || cv.isEmpty()) throw BusinessException.badRequest("Vui lòng đính kèm CV");
        record Pos(UUID id, UUID storeId) {
        }
        List<Pos> pos = jdbc.query("select p.id, c.store_id " + OPEN + " and p.code = :code",
                new MapSqlParameterSource("today", LocalDate.now()).addValue("code", code),
                (rs, i) -> new Pos(rs.getObject("id", UUID.class), rs.getObject("store_id", UUID.class)));
        if (pos.isEmpty()) throw BusinessException.notFound("Vị trí không tồn tại hoặc đã ngừng nhận hồ sơ");
        Pos position = pos.getFirst();
        String source = StringUtils.hasText(form.source()) && RecruitmentDtos.SOURCES.contains(form.source().trim().toUpperCase(Locale.ROOT))
                ? form.source().trim().toUpperCase(Locale.ROOT) : "WEBSITE";

        // Gộp vào ứng viên cũ trùng email (ưu tiên) hoặc số điện thoại.
        String email = form.email().trim().toLowerCase(Locale.ROOT);
        String digits = form.phone().replaceAll("\\D", "");
        String phoneKey = digits.length() >= 9 ? digits.substring(digits.length() - 9) : digits;
        List<UUID> existing = jdbc.queryForList("""
                select id from candidates where deleted_at is null and (lower(email) = :email or """
                + " " + CandidateService.PHONE_KEY.formatted("phone") + " = :phoneKey)"
                + " order by (lower(email) = :email) desc nulls last, created_at limit 1",
                new MapSqlParameterSource("email", email).addValue("phoneKey", phoneKey), UUID.class);
        UUID candidateId;
        if (existing.isEmpty()) {
            candidateId = candidates.insert(new CandidateRequest(form.fullName().trim(), email, form.phone().trim(),
                    form.dateOfBirth(), form.gender(), form.address(), form.currentTitle(), form.currentCompany(),
                    form.yearsExperience(), form.skills(), form.education(), null, source, "Biểu mẫu ứng tuyển", List.of(),
                    form.linkedinUrl(), form.portfolioUrl(), null, null, true), null, position.storeId());
        } else {
            candidateId = existing.getFirst();
            // Chỉ bổ sung thông tin còn trống, không ghi đè dữ liệu HR đã chỉnh.
            jdbc.update("""
                    update candidates set email = coalesce(email, :email), phone = coalesce(phone, :phone),
                        current_title = coalesce(current_title, :title), current_company = coalesce(current_company, :company),
                        years_experience = coalesce(years_experience, :years), linkedin_url = coalesce(linkedin_url, :linkedin),
                        portfolio_url = coalesce(portfolio_url, :portfolio), date_of_birth = coalesce(date_of_birth, :dob),
                        skills = case when cardinality(skills) = 0 then :skills else skills end,
                        updated_at = now() at time zone 'UTC'
                    where id = :id""", new MapSqlParameterSource("id", candidateId).addValue("email", email)
                    .addValue("phone", form.phone().trim()).addValue("title", ApplicationWorkflow.trim(form.currentTitle()))
                    .addValue("company", ApplicationWorkflow.trim(form.currentCompany())).addValue("years", form.yearsExperience())
                    .addValue("linkedin", ApplicationWorkflow.trim(form.linkedinUrl()))
                    .addValue("portfolio", ApplicationWorkflow.trim(form.portfolioUrl())).addValue("dob", form.dateOfBirth())
                    .addValue("skills", ApplicationWorkflow.toArray(form.skills())));
        }
        UUID applicationId = applications.createInternal(candidateId, position.id(), source, form.coverNote(), null, true, true);
        documents.upload(candidateId, cv, "CV", "Nộp qua biểu mẫu ứng tuyển", applicationId, null);
        if (coverLetter != null && !coverLetter.isEmpty())
            documents.upload(candidateId, coverLetter, "COVER_LETTER", null, applicationId, null);
        if (portfolio != null && !portfolio.isEmpty())
            documents.upload(candidateId, portfolio, "PORTFOLIO", null, applicationId, null);
        String appCode = jdbc.queryForObject("select code from job_applications where id = :id",
                new MapSqlParameterSource("id", applicationId), String.class);
        workflow.history(applicationId, "DOCUMENT", null, null, null,
                existing.isEmpty() ? "Ứng viên nộp hồ sơ qua trang tuyển dụng" : "Ứng viên (đã có trong hệ thống) nộp hồ sơ qua trang tuyển dụng",
                null, null);
        return new PublicApplyResult(appCode, "Cảm ơn bạn đã ứng tuyển! Mã hồ sơ: " + appCode
                + ". Bộ phận tuyển dụng sẽ liên hệ nếu hồ sơ phù hợp.");
    }

    /** Tối đa {@code maxPerWindow} lần nộp mỗi IP trong cửa sổ. */
    void throttle(String clientKey) {
        long now = clock.millis();
        Deque<Long> times = attempts.computeIfAbsent(clientKey == null ? "unknown" : clientKey, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > windowMillis) times.pollFirst();
            if (times.size() >= maxPerWindow)
                throw BusinessException.tooManyRequests("Bạn đã nộp quá nhiều hồ sơ, vui lòng thử lại sau", windowMillis / 1000);
            times.addLast(now);
        }
        if (attempts.size() > 10_000) attempts.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}
