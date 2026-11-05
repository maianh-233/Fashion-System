package com.fashionsystem.fashion_system.recruitment;

import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.time;
import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.trim;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CampaignDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CampaignRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.CampaignSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Member;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.MemberRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PositionDetail;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PositionRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.PositionSummary;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.StageStat;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

/** Chiến dịch tuyển dụng và các vị trí trong chiến dịch. */
@Service
@Transactional(readOnly = true)
public class CampaignService {
    static final String[] CAMPAIGN_STATUSES = {"DRAFT", "OPEN", "PAUSED", "CLOSED", "CANCELLED"};
    static final String[] POSITION_STATUSES = {"DRAFT", "OPEN", "PAUSED", "CLOSED", "FILLED"};
    /** Hồ sơ "đang trong quy trình" = chưa kết thúc và đã qua bước tiếp nhận. */
    static final String IN_PROCESS = "('SCREENING_PENDING','PROCESSING','QUALIFIED','INTERVIEW_PENDING','INTERVIEWING',"
            + "'EVALUATION_PENDING','ROUND_PASSED','ROUND_FAILED','OFFER_PENDING','OFFER_SENT','OFFER_ACCEPTED')";

    private final NamedParameterJdbcTemplate jdbc;
    private final RecruitmentAccess access;
    private final ApplicationWorkflow workflow;
    private final PipelineService pipelines;

    public CampaignService(NamedParameterJdbcTemplate jdbc, RecruitmentAccess access, ApplicationWorkflow workflow,
                           PipelineService pipelines) {
        this.jdbc = jdbc;
        this.access = access;
        this.workflow = workflow;
        this.pipelines = pipelines;
    }

    // ------------------------------------------------------------------ chiến dịch: đọc

    private static final String CAMPAIGN_SELECT = """
            select c.id, c.code, c.name, c.store_id, st.name as store_name, c.department_id, d.name as department_name,
                   c.owner_id, o.full_name as owner_name,
                   c.start_date, c.end_date, c.status, c.created_at,
                   (select count(*) from recruitment_positions p where p.campaign_id = c.id) as position_count,
                   (select count(*) from recruitment_positions p where p.campaign_id = c.id and p.status = 'OPEN') as open_position_count,
                   (select coalesce(sum(p.headcount), 0) from recruitment_positions p where p.campaign_id = c.id) as headcount,
                   (select count(*) from job_applications a join recruitment_positions p on p.id = a.position_id
                    where p.campaign_id = c.id and a.status = 'HIRED') as hired,
                   (select count(*) from job_applications a join recruitment_positions p on p.id = a.position_id
                    where p.campaign_id = c.id) as applications,
                   (select count(*) from job_applications a join recruitment_positions p on p.id = a.position_id
                    where p.campaign_id = c.id and a.status in """ + IN_PROCESS + """
            ) as in_process
            from recruitment_campaigns c
            left join stores st on st.id = c.store_id
            left join departments d on d.id = c.department_id
            left join users o on o.id = c.owner_id
            """;

    public Page<CampaignSummary> list(Actor actor, String keyword, String status, UUID departmentId, UUID storeId,
                                      Boolean headOffice, Pageable pageable) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor);
        StringBuilder where = new StringBuilder(" where ").append(RecruitmentAccess.campaignVisible("c"));
        if (storeId != null) {
            where.append(" and c.store_id = :filterStore");
            params.addValue("filterStore", storeId);
        } else if (Boolean.TRUE.equals(headOffice)) {
            where.append(" and c.store_id is null");
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" and (c.name ilike :kw or c.code ilike :kw)");
            params.addValue("kw", "%" + keyword.trim() + "%");
        }
        if (StringUtils.hasText(status)) {
            where.append(" and c.status = :status");
            params.addValue("status", status.trim().toUpperCase());
        }
        if (departmentId != null) {
            where.append(" and c.department_id = :dept");
            params.addValue("dept", departmentId);
        }
        Long total = jdbc.queryForObject("select count(*) from recruitment_campaigns c" + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<CampaignSummary> rows = jdbc.query(CAMPAIGN_SELECT + where
                + " order by case c.status when 'OPEN' then 0 when 'DRAFT' then 1 when 'PAUSED' then 2 else 3 end, c.created_at desc, c.id"
                + " limit :limit offset :offset", params, (rs, i) -> campaign(rs));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    public CampaignDetail detail(Actor actor, UUID id) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor).addValue("id", id);
        List<CampaignSummary> found = jdbc.query(CAMPAIGN_SELECT + " where c.id = :id", params, (rs, i) -> campaign(rs));
        if (found.isEmpty()) throw BusinessException.notFound("Chiến dịch không tồn tại");
        boolean visible = Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from recruitment_campaigns c where c.id = :id and "
                + RecruitmentAccess.campaignVisible("c") + ")", params, Boolean.class));
        if (!visible) throw BusinessException.forbidden("Bạn không được phân công vào chiến dịch này");
        String description = jdbc.queryForObject("select description from recruitment_campaigns where id = :id", params, String.class);
        return new CampaignDetail(found.getFirst(), description, members(id), positions(actor, id, null, null, null),
                access.canManageCampaign(actor, id));
    }

    List<Member> members(UUID campaignId) {
        return jdbc.query("""
                select m.user_id, u.full_name, u.username, m.member_role from recruitment_campaign_members m
                join users u on u.id = m.user_id where m.campaign_id = :id order by m.member_role, u.full_name""",
                new MapSqlParameterSource("id", campaignId), (rs, i) -> new Member(rs.getObject("user_id", UUID.class),
                        rs.getString("full_name"), rs.getString("username"), rs.getString("member_role")));
    }

    private static CampaignSummary campaign(ResultSet rs) throws SQLException {
        return new CampaignSummary(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getObject("store_id", UUID.class), rs.getString("store_name"), rs.getObject("department_id", UUID.class), rs.getString("department_name"), rs.getObject("owner_id", UUID.class),
                rs.getString("owner_name"), rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                rs.getString("status"), rs.getInt("position_count"), rs.getInt("open_position_count"), rs.getInt("headcount"),
                rs.getInt("hired"), rs.getInt("applications"), rs.getInt("in_process"), time(rs, "created_at"));
    }

    // ------------------------------------------------------------------ chiến dịch: ghi

    @Transactional
    public CampaignDetail create(Actor actor, CampaignRequest request) {
        if (!actor.admin() && !actor.manage()) throw BusinessException.forbidden("Bạn không có quyền tạo chiến dịch");
        validateCampaign(request);
        UUID store = resolveStore(actor, request.storeId());
        UUID owner = request.ownerId() == null ? actor.userId() : request.ownerId();
        access.requireAssignable(owner, store, "Người phụ trách");
        String status = request.status() == null ? "DRAFT"
                : ApplicationWorkflow.requireOneOf(request.status(), "Trạng thái", "DRAFT", "OPEN");
        UUID id = jdbc.queryForObject("""
                insert into recruitment_campaigns (code, name, store_id, department_id, owner_id, start_date, end_date, description, status, created_by)
                values (:code, :name, :store, :dept, :owner, :start, :end, :description, :status, :actor) returning id""",
                new MapSqlParameterSource("code", workflow.nextCode("CD", "recruitment_campaign_code_seq"))
                        .addValue("name", request.name().trim()).addValue("dept", request.departmentId())
                        .addValue("store", store).addValue("owner", owner).addValue("start", request.startDate())
                        .addValue("end", request.endDate())
                        .addValue("description", trim(request.description())).addValue("status", status)
                        .addValue("actor", actor.userId()), UUID.class);
        List<MemberRequest> members = new ArrayList<>(request.members() == null ? List.of() : request.members());
        // Recruiter tạo chiến dịch cho người khác phụ trách vẫn phải giữ quyền quản lý chiến dịch mình tạo.
        if (!actor.admin() && !owner.equals(actor.userId())
                && members.stream().noneMatch(m -> m.userId().equals(actor.userId())))
            members.add(new MemberRequest(actor.userId(), "RECRUITER"));
        replaceMembers(id, store, members);
        return detail(actor, id);
    }

    /**
     * Cửa hàng của chiến dịch: nhân viên cửa hàng luôn tuyển cho cửa hàng mình; nhân viên cấp chuỗi chọn cửa hàng hoặc
     * để trống (văn phòng).
     */
    private UUID resolveStore(Actor actor, UUID requested) {
        if (!actor.global()) {
            if (requested != null && !requested.equals(actor.storeId()))
                throw BusinessException.forbidden("Bạn chỉ được tuyển dụng cho cửa hàng của mình");
            return actor.storeId();
        }
        if (requested != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from stores where id = :id and coalesce(active, true))",
                new MapSqlParameterSource("id", requested), Boolean.class)))
            throw BusinessException.notFound("Cửa hàng không tồn tại hoặc ngừng hoạt động");
        return requested;
    }

    @Transactional
    public CampaignDetail update(Actor actor, UUID id, CampaignRequest request) {
        access.requireManageCampaign(actor, id);
        validateCampaign(request);
        UUID currentStore = access.storeOfCampaign(id);
        // Chỉ nhân viên cấp chuỗi được chuyển chiến dịch sang cửa hàng khác.
        UUID store = actor.global() ? resolveStore(actor, request.storeId()) : currentStore;
        if (!java.util.Objects.equals(store, currentStore) && Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from job_applications a join recruitment_positions p on p.id = a.position_id where p.campaign_id = :id)",
                new MapSqlParameterSource("id", id), Boolean.class)))
            throw BusinessException.invalidState("Chiến dịch đã có hồ sơ, không thể đổi cửa hàng");
        UUID owner = request.ownerId();
        if (owner != null) access.requireAssignable(owner, store, "Người phụ trách");
        jdbc.update("""
                update recruitment_campaigns set name = :name, store_id = :store, department_id = :dept, owner_id = coalesce(:owner, owner_id),
                    start_date = :start, end_date = :end, description = :description, updated_at = now() at time zone 'UTC'
                where id = :id""",
                new MapSqlParameterSource("id", id).addValue("name", request.name().trim()).addValue("dept", request.departmentId())
                        .addValue("store", store).addValue("owner", owner).addValue("start", request.startDate()).addValue("end", request.endDate())
                        .addValue("description", trim(request.description())));
        if (request.members() != null) replaceMembers(id, store, request.members());
        if (StringUtils.hasText(request.status())) changeStatus(actor, id, request.status());
        return detail(actor, id);
    }

    @Transactional
    public CampaignDetail changeStatus(Actor actor, UUID id, String status) {
        access.requireManageCampaign(actor, id);
        String target = ApplicationWorkflow.requireOneOf(status, "Trạng thái", CAMPAIGN_STATUSES);
        String current = jdbc.queryForObject("select status from recruitment_campaigns where id = :id for update",
                new MapSqlParameterSource("id", id), String.class);
        if (target.equals(current)) return detail(actor, id);
        if ("CLOSED".equals(current) || "CANCELLED".equals(current)) {
            if (!"OPEN".equals(target)) throw BusinessException.invalidState("Chiến dịch đã đóng chỉ có thể mở lại");
        }
        if ("DRAFT".equals(target) && !"DRAFT".equals(current))
            throw BusinessException.invalidState("Không thể đưa chiến dịch về nháp");
        jdbc.update("""
                update recruitment_campaigns set status = :status, updated_at = now() at time zone 'UTC',
                    closed_at = case when :status in ('CLOSED', 'CANCELLED') then now() at time zone 'UTC' else null end
                where id = :id""", new MapSqlParameterSource("id", id).addValue("status", target));
        return detail(actor, id);
    }

    @Transactional
    public void delete(Actor actor, UUID id) {
        access.requireManageCampaign(actor, id);
        Boolean hasApplications = jdbc.queryForObject("""
                select exists (select 1 from job_applications a join recruitment_positions p on p.id = a.position_id
                               where p.campaign_id = :id)""", new MapSqlParameterSource("id", id), Boolean.class);
        if (Boolean.TRUE.equals(hasApplications))
            throw BusinessException.invalidState("Chiến dịch đã có hồ sơ ứng tuyển, hãy đóng hoặc hủy thay vì xóa");
        jdbc.update("delete from recruitment_campaigns where id = :id", new MapSqlParameterSource("id", id));
    }

    private void validateCampaign(CampaignRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate()))
            throw BusinessException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        if (request.departmentId() != null) requireRow("departments", request.departmentId(), "Phòng ban không tồn tại");
    }

    private void replaceMembers(UUID campaignId, UUID store, List<MemberRequest> members) {
        jdbc.update("delete from recruitment_campaign_members where campaign_id = :id", new MapSqlParameterSource("id", campaignId));
        Map<UUID, String> unique = new LinkedHashMap<>();
        for (MemberRequest m : members) {
            unique.put(m.userId(), ApplicationWorkflow.requireOneOf(m.role(), "Vai trò thành viên", "RECRUITER", "HIRING_MANAGER"));
        }
        for (var entry : unique.entrySet()) {
            access.requireAssignable(entry.getKey(), store, "Thành viên chiến dịch");
            jdbc.update("insert into recruitment_campaign_members (campaign_id, user_id, member_role) values (:c, :u, :r)",
                    new MapSqlParameterSource("c", campaignId).addValue("u", entry.getKey()).addValue("r", entry.getValue()));
        }
    }

    // ------------------------------------------------------------------ vị trí: đọc

    private static final String POSITION_SELECT = """
            select p.id, p.code, p.title, p.campaign_id, c.name as campaign_name, c.status as campaign_status,
                   c.store_id, st.name as store_name,
                   p.department_id, d.name as department_name, p.hiring_manager_id, hm.full_name as hiring_manager_name,
                   p.headcount, p.deadline, p.status, p.is_public,
                   (select count(*) from job_applications a where a.position_id = p.id and a.status = 'HIRED') as hired,
                   (select count(*) from job_applications a where a.position_id = p.id) as applications,
                   (select count(*) from job_applications a where a.position_id = p.id and a.status in """ + IN_PROCESS + """
            ) as in_process,
                   (select count(*) from recruitment_stages s where s.position_id = p.id) as stage_count
            from recruitment_positions p
            join recruitment_campaigns c on c.id = p.campaign_id
            left join stores st on st.id = c.store_id
            left join departments d on d.id = p.department_id
            left join users hm on hm.id = p.hiring_manager_id
            """;

    public List<PositionSummary> positions(Actor actor, UUID campaignId, String status, String keyword, Boolean openOnly) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor);
        StringBuilder where = new StringBuilder(" where ").append(RecruitmentAccess.positionVisible("p", "c"));
        if (campaignId != null) {
            where.append(" and p.campaign_id = :campaign");
            params.addValue("campaign", campaignId);
        }
        if (StringUtils.hasText(status)) {
            where.append(" and p.status = :status");
            params.addValue("status", status.trim().toUpperCase());
        }
        if (Boolean.TRUE.equals(openOnly)) where.append(" and p.status = 'OPEN' and c.status = 'OPEN'");
        if (StringUtils.hasText(keyword)) {
            where.append(" and (p.title ilike :kw or p.code ilike :kw)");
            params.addValue("kw", "%" + keyword.trim() + "%");
        }
        return jdbc.query(POSITION_SELECT + where + " order by c.created_at desc, p.created_at, p.id", params, (rs, i) -> position(rs));
    }

    public PositionDetail position(Actor actor, UUID id) {
        access.requireViewPosition(actor, id);
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        PositionSummary summary = jdbc.query(POSITION_SELECT + " where p.id = :id", params, (rs, i) -> position(rs)).getFirst();
        List<StageStat> stats = jdbc.query("""
                select s.id,
                       count(r.*) filter (where r.status is not null) as total,
                       count(*) filter (where r.status = 'PENDING') as pending,
                       count(*) filter (where r.status in ('IN_PROGRESS', 'AWAITING_EVALUATION')) as in_progress,
                       count(*) filter (where r.status = 'PASSED') as passed,
                       count(*) filter (where r.status = 'FAILED') as failed,
                       (select count(*) from job_applications a where a.current_stage_id = s.id
                          and a.status not in ('REJECTED', 'WITHDRAWN', 'HIRED')) as current
                from recruitment_stages s left join application_stage_results r on r.stage_id = s.id
                where s.position_id = :id group by s.id, s.sort_order order by s.sort_order""", params,
                (rs, i) -> new StageStat(rs.getObject("id", UUID.class), rs.getInt("total"), rs.getInt("pending"),
                        rs.getInt("in_progress"), rs.getInt("passed"), rs.getInt("failed"), rs.getInt("current")));
        return jdbc.query("select * from recruitment_positions where id = :id", params, (rs, i) -> new PositionDetail(summary,
                rs.getObject("org_position_id", UUID.class), rs.getString("employment_type"), rs.getString("work_location"),
                rs.getString("job_description"), rs.getString("requirements"), rs.getString("benefits"),
                ApplicationWorkflow.textArray(rs, "skills"), rs.getBigDecimal("experience_min"), rs.getBigDecimal("experience_max"),
                rs.getBigDecimal("salary_min"), rs.getBigDecimal("salary_max"), rs.getBoolean("salary_negotiable"),
                pipelines.stages(id), stats, access.canManagePosition(actor, id))).getFirst();
    }

    static PositionSummary position(ResultSet rs) throws SQLException {
        return new PositionSummary(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("title"),
                rs.getObject("campaign_id", UUID.class), rs.getString("campaign_name"), rs.getString("campaign_status"),
                rs.getObject("store_id", UUID.class), rs.getString("store_name"), rs.getObject("department_id", UUID.class), rs.getString("department_name"),
                rs.getObject("hiring_manager_id", UUID.class), rs.getString("hiring_manager_name"), rs.getInt("headcount"),
                rs.getInt("hired"), rs.getInt("applications"), rs.getInt("in_process"),
                rs.getObject("deadline", LocalDate.class), rs.getString("status"), rs.getBoolean("is_public"),
                rs.getInt("stage_count"));
    }

    // ------------------------------------------------------------------ vị trí: ghi

    @Transactional
    public PositionDetail createPosition(Actor actor, UUID campaignId, PositionRequest request) {
        access.requireManageCampaign(actor, campaignId);
        validatePosition(request, access.storeOfCampaign(campaignId));
        String status = StringUtils.hasText(request.status())
                ? ApplicationWorkflow.requireOneOf(request.status(), "Trạng thái", "DRAFT", "OPEN") : "OPEN";
        UUID id = jdbc.queryForObject("""
                insert into recruitment_positions (campaign_id, code, title, department_id, org_position_id, hiring_manager_id,
                    headcount, employment_type, work_location, job_description, requirements, benefits, skills,
                    experience_min, experience_max, salary_min, salary_max, salary_negotiable, deadline, status, is_public, created_by)
                values (:campaign, :code, :title, :dept, :orgPosition, :manager, :headcount, :employmentType, :location,
                    :jd, :requirements, :benefits, :skills, :expMin, :expMax, :salMin, :salMax, :negotiable, :deadline,
                    :status, :public, :actor) returning id""",
                positionParams(request).addValue("campaign", campaignId)
                        .addValue("code", workflow.nextCode("VT", "recruitment_position_code_seq"))
                        .addValue("status", status).addValue("actor", actor.userId()), UUID.class);
        if (request.templateId() != null) pipelines.applyTemplateInternal(id, request.templateId());
        return position(actor, id);
    }

    @Transactional
    public PositionDetail updatePosition(Actor actor, UUID id, PositionRequest request) {
        access.requireManagePosition(actor, id);
        validatePosition(request, access.storeOfPosition(id));
        jdbc.update("""
                update recruitment_positions set title = :title, department_id = :dept, org_position_id = :orgPosition,
                    hiring_manager_id = :manager, headcount = :headcount, employment_type = :employmentType,
                    work_location = :location, job_description = :jd, requirements = :requirements, benefits = :benefits,
                    skills = :skills, experience_min = :expMin, experience_max = :expMax, salary_min = :salMin,
                    salary_max = :salMax, salary_negotiable = :negotiable, deadline = :deadline, is_public = :public,
                    updated_at = now() at time zone 'UTC'
                where id = :id""", positionParams(request).addValue("id", id));
        if (StringUtils.hasText(request.status())) changePositionStatus(actor, id, request.status());
        return position(actor, id);
    }

    @Transactional
    public PositionDetail changePositionStatus(Actor actor, UUID id, String status) {
        access.requireManagePosition(actor, id);
        String target = ApplicationWorkflow.requireOneOf(status, "Trạng thái", POSITION_STATUSES);
        jdbc.update("update recruitment_positions set status = :status, updated_at = now() at time zone 'UTC' where id = :id",
                new MapSqlParameterSource("id", id).addValue("status", target));
        return position(actor, id);
    }

    @Transactional
    public void deletePosition(Actor actor, UUID id) {
        access.requireManagePosition(actor, id);
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from job_applications where position_id = :id)",
                new MapSqlParameterSource("id", id), Boolean.class)))
            throw BusinessException.invalidState("Vị trí đã có hồ sơ ứng tuyển, hãy đóng thay vì xóa");
        jdbc.update("delete from recruitment_positions where id = :id", new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource positionParams(PositionRequest r) {
        return new MapSqlParameterSource("title", r.title().trim()).addValue("dept", r.departmentId())
                .addValue("orgPosition", r.orgPositionId()).addValue("manager", r.hiringManagerId())
                .addValue("headcount", r.headcount()).addValue("employmentType", trim(r.employmentType()))
                .addValue("location", trim(r.workLocation())).addValue("jd", trim(r.jobDescription()))
                .addValue("requirements", trim(r.requirements())).addValue("benefits", trim(r.benefits()))
                .addValue("skills", ApplicationWorkflow.toArray(r.skills())).addValue("expMin", r.experienceMin())
                .addValue("expMax", r.experienceMax()).addValue("salMin", r.salaryMin()).addValue("salMax", r.salaryMax())
                .addValue("negotiable", Boolean.TRUE.equals(r.salaryNegotiable())).addValue("deadline", r.deadline())
                .addValue("public", r.isPublic() == null || r.isPublic());
    }

    private void validatePosition(PositionRequest r, UUID store) {
        if (gt(r.salaryMin(), r.salaryMax())) throw BusinessException.badRequest("Lương tối thiểu không được lớn hơn lương tối đa");
        if (gt(r.experienceMin(), r.experienceMax()))
            throw BusinessException.badRequest("Kinh nghiệm tối thiểu không được lớn hơn tối đa");
        if (r.departmentId() != null) requireRow("departments", r.departmentId(), "Phòng ban không tồn tại");
        if (r.orgPositionId() != null) requireRow("positions", r.orgPositionId(), "Chức danh không tồn tại");
        if (r.hiringManagerId() != null) access.requireAssignable(r.hiringManagerId(), store, "Hiring manager");
    }

    private static boolean gt(BigDecimal a, BigDecimal b) {
        return a != null && b != null && a.compareTo(b) > 0;
    }

    void requireUser(UUID id) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from users where id = :id and deleted_at is null and coalesce(active, true))",
                new MapSqlParameterSource("id", id), Boolean.class)))
            throw BusinessException.notFound("Nhân viên được chọn không tồn tại hoặc đã nghỉ");
    }

    private void requireRow(String table, UUID id, String message) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from " + table + " where id = :id)",
                new MapSqlParameterSource("id", id), Boolean.class))) throw BusinessException.notFound(message);
    }
}
