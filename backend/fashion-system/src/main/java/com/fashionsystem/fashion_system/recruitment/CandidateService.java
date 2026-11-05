package com.fashionsystem.fashion_system.recruitment;

import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.textArray;
import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.time;
import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.trim;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.ApplicationSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CandidateDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CandidateRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CandidateSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Document;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.DuplicateMatch;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.SignedUrl;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hồ sơ ứng viên: thông tin cá nhân, liên hệ, kinh nghiệm, kỹ năng, nguồn; tìm kiếm / lọc; kiểm tra trùng theo email và
 * số điện thoại (9 số cuối, nên "0912..." và "+84912..." là một). Một ứng viên có nhiều hồ sơ ứng tuyển.
 */
@Service
@Transactional(readOnly = true)
public class CandidateService {
    /** Số điện thoại chỉ giữ 9 chữ số cuối để so trùng. */
    static final String PHONE_KEY = "right(regexp_replace(coalesce(%s, ''), '\\D', '', 'g'), 9)";

    private final NamedParameterJdbcTemplate jdbc;
    private final RecruitmentAccess access;
    private final ApplicationWorkflow workflow;
    private final ApplicationService applications;
    private final CandidateDocumentService documents;

    public CandidateService(NamedParameterJdbcTemplate jdbc, RecruitmentAccess access, ApplicationWorkflow workflow,
                            ApplicationService applications, CandidateDocumentService documents) {
        this.jdbc = jdbc;
        this.access = access;
        this.workflow = workflow;
        this.applications = applications;
        this.documents = documents;
    }

    public record Filter(String keyword, String source, String skill, String tag, String status, UUID positionId,
                         UUID campaignId, UUID storeId, Boolean hasCv) {
    }

    // ------------------------------------------------------------------ đọc

    public Page<CandidateSummary> list(Actor actor, Filter f, Pageable pageable) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor);
        StringBuilder where = new StringBuilder(" where ").append(RecruitmentAccess.candidateVisible("cd"));
        if (StringUtils.hasText(f.keyword())) {
            where.append(" and (cd.full_name ilike :kw or cd.email ilike :kw or cd.phone ilike :kw or cd.code ilike :kw"
                    + " or cd.current_title ilike :kw or exists (select 1 from unnest(cd.skills) sk where sk ilike :kw))");
            params.addValue("kw", "%" + f.keyword().trim() + "%");
        }
        if (StringUtils.hasText(f.source())) { where.append(" and cd.source = :source"); params.addValue("source", f.source().trim().toUpperCase()); }
        if (StringUtils.hasText(f.skill())) {
            where.append(" and exists (select 1 from unnest(cd.skills) sk where lower(sk) = lower(:skill))");
            params.addValue("skill", f.skill().trim());
        }
        if (StringUtils.hasText(f.tag())) {
            where.append(" and exists (select 1 from unnest(cd.tags) tg where lower(tg) = lower(:tag))");
            params.addValue("tag", f.tag().trim());
        }
        if (f.storeId() != null) { where.append(" and cd.store_id = :filterStore"); params.addValue("filterStore", f.storeId()); }
        if (Boolean.TRUE.equals(f.hasCv())) where.append(" and exists (select 1 from candidate_documents d where d.candidate_id = cd.id and d.doc_type = 'CV' and d.deleted_at is null)");
        if (Boolean.FALSE.equals(f.hasCv())) where.append(" and not exists (select 1 from candidate_documents d where d.candidate_id = cd.id and d.doc_type = 'CV' and d.deleted_at is null)");
        if (f.positionId() != null || f.campaignId() != null || StringUtils.hasText(f.status())) {
            where.append(" and exists (select 1 from job_applications fa join recruitment_positions fp on fp.id = fa.position_id where fa.candidate_id = cd.id");
            if (f.positionId() != null) { where.append(" and fa.position_id = :position"); params.addValue("position", f.positionId()); }
            if (f.campaignId() != null) { where.append(" and fp.campaign_id = :campaign"); params.addValue("campaign", f.campaignId()); }
            if (StringUtils.hasText(f.status())) {
                where.append(" and fa.status = :status");
                params.addValue("status", ApplicationWorkflow.parse(ApplicationStatus.class, f.status(), "Trạng thái").name());
            }
            where.append(")");
        }
        Long total = jdbc.queryForObject("select count(*) from candidates cd" + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<CandidateSummary> rows = jdbc.query("""
                select cd.*, st.name as store_name,
                       (select count(*) from job_applications a where a.candidate_id = cd.id) as application_count,
                       la.status as latest_status, la.title as latest_position,
                       exists (select 1 from candidate_documents d where d.candidate_id = cd.id and d.doc_type = 'CV' and d.deleted_at is null) as has_cv,
                       (select count(*) from candidates o where o.id <> cd.id and o.deleted_at is null and (
                            (cd.email is not null and lower(o.email) = lower(cd.email))
                            or (length(""" + PHONE_KEY.formatted("cd.phone") + ") = 9 and " + PHONE_KEY.formatted("o.phone")
                + " = " + PHONE_KEY.formatted("cd.phone") + """
                       ))) as duplicate_count
                from candidates cd
                left join stores st on st.id = cd.store_id
                left join lateral (select a.status, p.title from job_applications a join recruitment_positions p on p.id = a.position_id
                                   where a.candidate_id = cd.id order by a.applied_at desc limit 1) la on true
                """ + where + " order by cd.created_at desc, cd.id limit :limit offset :offset", params, (rs, i) -> {
            String latest = rs.getString("latest_status");
            return new CandidateSummary(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("full_name"),
                    rs.getString("email"), rs.getString("phone"), rs.getString("current_title"), rs.getBigDecimal("years_experience"),
                    textArray(rs, "skills"), rs.getString("source"), textArray(rs, "tags"), rs.getInt("application_count"),
                    latest == null ? null : ApplicationStatus.valueOf(latest).label(), rs.getString("latest_position"),
                    rs.getBoolean("has_cv"), rs.getInt("duplicate_count"), rs.getString("store_name"), time(rs, "created_at"));
        });
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    public CandidateDetail detail(Actor actor, UUID id) {
        access.requireViewCandidate(actor, id);
        List<ApplicationSummary> apps = applications.list(actor, new ApplicationService.Filter(null, null, id, null, null,
                null, null, null, false), Pageable.ofSize(100)).getContent();
        boolean canManage = actor.admin() || actor.manage();
        return jdbc.query("""
                select cd.*, st.name as store_name, u.full_name as created_by_name, e.employee_code from candidates cd
                left join stores st on st.id = cd.store_id left join users u on u.id = cd.created_by
                left join users e on e.id = cd.employee_id where cd.id = :id""",
                new MapSqlParameterSource("id", id), (rs, i) -> new CandidateDetail(rs.getObject("id", UUID.class),
                        rs.getString("code"), rs.getString("full_name"), rs.getString("email"), rs.getString("phone"),
                        rs.getObject("date_of_birth", LocalDate.class), rs.getString("gender"), rs.getString("address"),
                        rs.getString("current_title"), rs.getString("current_company"), rs.getBigDecimal("years_experience"),
                        textArray(rs, "skills"), rs.getString("education"), rs.getString("summary"), rs.getString("source"),
                        rs.getString("source_detail"), textArray(rs, "tags"), rs.getString("linkedin_url"),
                        rs.getString("portfolio_url"), canManage ? rs.getString("notes") : null,
                        rs.getObject("store_id", UUID.class), rs.getString("store_name"), rs.getString("created_by_name"),
                        time(rs, "created_at"), time(rs, "updated_at"), documents.list(id), apps,
                        duplicates(actor, rs.getString("email"), rs.getString("phone"), id),
                        rs.getObject("employee_id", UUID.class), rs.getString("employee_code"), canManage)).getFirst();
    }

    /** Ứng viên khác (người dùng được xem) trùng email hoặc số điện thoại. */
    public List<DuplicateMatch> duplicates(Actor actor, String email, String phone, UUID excludeId) {
        String normalizedEmail = StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        String phoneKey = digits.length() >= 9 ? digits.substring(digits.length() - 9) : null;
        if (normalizedEmail == null && phoneKey == null) return List.of();
        MapSqlParameterSource params = RecruitmentAccess.params(actor).addValue("email", normalizedEmail, java.sql.Types.VARCHAR)
                .addValue("phoneKey", phoneKey, java.sql.Types.VARCHAR).addValue("exclude", excludeId, java.sql.Types.OTHER);
        return jdbc.query("""
                select cd.id, cd.code, cd.full_name, cd.email, cd.phone, cd.created_at,
                       (cd.email is not null and lower(cd.email) = :email) as email_match,
                       (""" + PHONE_KEY.formatted("cd.phone") + """
                 = :phoneKey) as phone_match,
                       (select count(*) from job_applications a where a.candidate_id = cd.id) as application_count
                from candidates cd
                where (cast(:exclude as uuid) is null or cd.id <> :exclude)
                  and ((cd.email is not null and lower(cd.email) = :email) or (""" + PHONE_KEY.formatted("cd.phone") + """
                 = :phoneKey))
                  and """ + " " + RecruitmentAccess.candidateVisible("cd") + " order by cd.created_at", params, (rs, i) -> {
            List<String> matched = new ArrayList<>();
            if (rs.getBoolean("email_match")) matched.add("EMAIL");
            if (rs.getBoolean("phone_match")) matched.add("PHONE");
            return new DuplicateMatch(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("full_name"),
                    rs.getString("email"), rs.getString("phone"), matched, rs.getInt("application_count"), time(rs, "created_at"));
        });
    }

    // ------------------------------------------------------------------ ghi

    /** Tạo ứng viên (cảnh báo trùng trừ khi bỏ qua) và ứng tuyển luôn vào các vị trí được chọn. */
    @Transactional
    public CandidateDetail create(Actor actor, CandidateRequest request) {
        if (!actor.admin() && !actor.manage()) throw BusinessException.forbidden("Bạn không có quyền thêm ứng viên");
        validate(request);
        if (!Boolean.TRUE.equals(request.ignoreDuplicates())) {
            List<DuplicateMatch> matches = duplicates(actor, request.email(), request.phone(), null);
            if (!matches.isEmpty()) {
                DuplicateMatch m = matches.getFirst();
                throw BusinessException.conflict("Có thể trùng ứng viên " + m.code() + " – " + m.fullName()
                        + " (" + String.join(", ", m.matchedOn().stream().map(x -> x.equals("EMAIL") ? "email" : "số điện thoại").toList())
                        + "). Mở hồ sơ cũ hoặc chọn \"Vẫn tạo mới\".");
            }
        }
        UUID id = insert(request, actor.userId(), actor.storeId());
        for (UUID positionId : new LinkedHashSet<>(request.positionIds() == null ? List.<UUID>of() : request.positionIds())) {
            access.requireManagePosition(actor, positionId);
            applications.createInternal(id, positionId, request.source(), null, actor.userId(), false, false);
        }
        return detail(actor, id);
    }

    UUID insert(CandidateRequest r, UUID actor, UUID storeId) {
        return jdbc.queryForObject("""
                insert into candidates (code, full_name, email, phone, date_of_birth, gender, address, current_title,
                    current_company, years_experience, skills, education, summary, source, source_detail, tags, linkedin_url,
                    portfolio_url, notes, store_id, created_by)
                values (:code, :name, :email, :phone, :dob, :gender, :address, :title, :company, :years, :skills, :education,
                    :summary, :source, :sourceDetail, :tags, :linkedin, :portfolio, :notes, :store, :actor) returning id""",
                params(r).addValue("code", workflow.nextCode("UV", "candidate_code_seq")).addValue("store", storeId)
                        .addValue("actor", actor), UUID.class);
    }

    @Transactional
    public CandidateDetail update(Actor actor, UUID id, CandidateRequest request) {
        access.requireManageCandidate(actor, id);
        validate(request);
        jdbc.update("""
                update candidates set full_name = :name, email = :email, phone = :phone, date_of_birth = :dob, gender = :gender,
                    address = :address, current_title = :title, current_company = :company, years_experience = :years,
                    skills = :skills, education = :education, summary = :summary, source = :source, source_detail = :sourceDetail,
                    tags = :tags, linkedin_url = :linkedin, portfolio_url = :portfolio, notes = :notes,
                    updated_at = now() at time zone 'UTC'
                where id = :id""", params(request).addValue("id", id));
        return detail(actor, id);
    }

    /** Xóa mềm (chỉ quản trị); ứng viên đang có hồ sơ chưa kết thúc thì không xóa được. */
    @Transactional
    public void delete(Actor actor, UUID id) {
        access.requireViewCandidate(actor, id);
        UUID store = jdbc.queryForObject("select store_id from candidates where id = :id", new MapSqlParameterSource("id", id), UUID.class);
        if (!actor.adminOf(store)) throw BusinessException.forbidden("Chỉ HR Admin được xóa ứng viên");
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from job_applications where candidate_id = :id
                               and status not in ('REJECTED', 'WITHDRAWN', 'HIRED', 'OFFER_DECLINED'))""",
                new MapSqlParameterSource("id", id), Boolean.class)))
            throw BusinessException.invalidState("Ứng viên còn hồ sơ đang xử lý");
        jdbc.update("update candidates set deleted_at = now() at time zone 'UTC' where id = :id", new MapSqlParameterSource("id", id));
    }

    // ------------------------------------------------------------------ tài liệu

    @Transactional
    public Document upload(Actor actor, UUID candidateId, MultipartFile file, String docType, String note, UUID applicationId) {
        access.requireManageCandidate(actor, candidateId);
        Document doc = documents.upload(candidateId, file, docType, note, applicationId, actor.userId());
        for (UUID app : jdbc.queryForList("select id from job_applications where candidate_id = :c and (cast(:a as uuid) is null or id = :a)",
                new MapSqlParameterSource("c", candidateId).addValue("a", applicationId, java.sql.Types.OTHER), UUID.class)) {
            workflow.history(app, "DOCUMENT", null, null, null, "Tải lên " + docType(doc.docType()) + " v" + doc.version()
                    + ": " + doc.fileName(), null, actor.userId());
        }
        return doc;
    }

    public SignedUrl documentUrl(Actor actor, UUID candidateId, UUID documentId, boolean inline) {
        access.requireViewCandidate(actor, candidateId);
        return documents.signedUrl(candidateId, documentId, inline);
    }

    @Transactional
    public void deleteDocument(Actor actor, UUID candidateId, UUID documentId) {
        access.requireManageCandidate(actor, candidateId);
        documents.delete(candidateId, documentId);
    }

    static String docType(String type) {
        return switch (type) {
            case "CV" -> "CV";
            case "COVER_LETTER" -> "thư xin việc";
            case "PORTFOLIO" -> "portfolio";
            case "CERTIFICATE" -> "chứng chỉ";
            default -> "tài liệu";
        };
    }

    private static void validate(CandidateRequest r) {
        if (!StringUtils.hasText(r.email()) && !StringUtils.hasText(r.phone()))
            throw BusinessException.badRequest("Cần ít nhất email hoặc số điện thoại");
        if (StringUtils.hasText(r.phone()) && !r.phone().trim().matches("^\\+?[0-9 .()-]{8,20}$"))
            throw BusinessException.badRequest("Số điện thoại không hợp lệ");
        if (StringUtils.hasText(r.source()) && !RecruitmentDtos.SOURCES.contains(r.source().trim().toUpperCase()))
            throw BusinessException.badRequest("Nguồn ứng tuyển không hợp lệ");
        if (r.dateOfBirth() != null && r.dateOfBirth().isAfter(LocalDate.now().minusYears(15)))
            throw BusinessException.badRequest("Ngày sinh không hợp lệ");
    }

    private static MapSqlParameterSource params(CandidateRequest r) {
        return new MapSqlParameterSource("name", r.fullName().trim())
                .addValue("email", StringUtils.hasText(r.email()) ? r.email().trim().toLowerCase(Locale.ROOT) : null)
                .addValue("phone", trim(r.phone())).addValue("dob", r.dateOfBirth()).addValue("gender", trim(r.gender()))
                .addValue("address", trim(r.address())).addValue("title", trim(r.currentTitle()))
                .addValue("company", trim(r.currentCompany())).addValue("years", r.yearsExperience())
                .addValue("skills", ApplicationWorkflow.toArray(r.skills())).addValue("education", trim(r.education()))
                .addValue("summary", trim(r.summary()))
                .addValue("source", StringUtils.hasText(r.source()) ? r.source().trim().toUpperCase() : "OTHER")
                .addValue("sourceDetail", trim(r.sourceDetail())).addValue("tags", ApplicationWorkflow.toArray(r.tags()))
                .addValue("linkedin", trim(r.linkedinUrl())).addValue("portfolio", trim(r.portfolioUrl()))
                .addValue("notes", trim(r.notes()));
    }
}
