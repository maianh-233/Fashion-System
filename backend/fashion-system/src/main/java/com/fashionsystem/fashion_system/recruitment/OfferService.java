package com.fashionsystem.fashion_system.recruitment;

import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.time;
import static com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.trim;

import com.fashionsystem.fashion_system.dto.employee.CreateEmployeeRequest;
import com.fashionsystem.fashion_system.dto.employee.CreateEmployeeResponse;
import com.fashionsystem.fashion_system.entity.EmploymentType;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.recruitment.ApplicationWorkflow.AppState;
import com.fashionsystem.fashion_system.recruitment.RecruitmentAccess.Actor;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.HireRequest;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.HireResult;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.Offer;
import com.fashionsystem.fashion_system.recruitment.RecruitmentDtos.OfferRequest;
import com.fashionsystem.fashion_system.service.EmployeeAdministrationService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Offer: Nháp → Chờ duyệt → Đã duyệt (người duyệt: hiring manager / HR Admin) → Đã gửi → Ứng viên chấp nhận / từ chối
 * (hoặc hết hạn). Chấp nhận rồi thì chuyển ứng viên sang module Nhân sự (tạo tài khoản nhân viên) → "Đã tuyển dụng".
 */
@Service
@Transactional(readOnly = true)
public class OfferService {
    /** Hồ sơ ở các trạng thái này mới tạo được offer. */
    private static final Set<ApplicationStatus> OFFERABLE = EnumSet.of(ApplicationStatus.QUALIFIED,
            ApplicationStatus.ROUND_PASSED, ApplicationStatus.OFFER_PENDING, ApplicationStatus.OFFER_DECLINED);

    private final NamedParameterJdbcTemplate jdbc;
    private final RecruitmentAccess access;
    private final ApplicationWorkflow workflow;
    private final EmployeeAdministrationService employees;
    private final RecruitmentNotifier notifier;
    private final RecruitmentMailer mailer;
    private static final java.time.format.DateTimeFormatter DAY = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public OfferService(NamedParameterJdbcTemplate jdbc, RecruitmentAccess access, ApplicationWorkflow workflow,
                        EmployeeAdministrationService employees, RecruitmentNotifier notifier, RecruitmentMailer mailer) {
        this.mailer = mailer;
        this.jdbc = jdbc;
        this.access = access;
        this.workflow = workflow;
        this.employees = employees;
        this.notifier = notifier;
    }

    private static final String SELECT = """
            select o.*, a.code as application_code, a.position_id, cd.full_name as candidate_name, p.title as position_title,
                   ap.full_name as approved_by_name, cb.full_name as created_by_name
            from recruitment_offers o
            join job_applications a on a.id = o.application_id
            join candidates cd on cd.id = a.candidate_id
            join recruitment_positions p on p.id = a.position_id
            join recruitment_campaigns c on c.id = p.campaign_id
            left join users ap on ap.id = o.approved_by
            left join users cb on cb.id = o.created_by
            """;

    List<Offer> query(Actor actor, String tail, MapSqlParameterSource params) {
        record Row(Offer offer, UUID positionId) {
        }
        List<Row> rows = jdbc.query(SELECT + tail, params, (rs, i) -> new Row(new Offer(rs.getObject("id", UUID.class),
                rs.getObject("application_id", UUID.class), rs.getString("application_code"), rs.getString("candidate_name"),
                rs.getString("position_title"), rs.getString("job_title"), rs.getBigDecimal("salary"),
                rs.getObject("start_date", LocalDate.class), rs.getObject("response_deadline", LocalDate.class),
                rs.getString("note"), rs.getString("status"), rs.getString("approved_by_name"), time(rs, "approved_at"),
                time(rs, "sent_at"), time(rs, "responded_at"), rs.getString("decline_reason"), rs.getString("created_by_name"),
                time(rs, "created_at"), false, false), rs.getObject("position_id", UUID.class)));
        return rows.stream().map(r -> {
            Offer o = r.offer();
            return new Offer(o.id(), o.applicationId(), o.applicationCode(), o.candidateName(), o.positionTitle(), o.jobTitle(),
                    o.salary(), o.startDate(), o.responseDeadline(), o.note(), o.status(), o.approvedByName(), o.approvedAt(),
                    o.sentAt(), o.respondedAt(), o.declineReason(), o.createdByName(), o.createdAt(),
                    access.canApprovePosition(actor, r.positionId()), access.canManagePosition(actor, r.positionId()));
        }).toList();
    }

    public List<Offer> forApplication(Actor actor, UUID applicationId) {
        return query(actor, " where o.application_id = :a order by o.created_at desc", new MapSqlParameterSource("a", applicationId));
    }

    /** Offer người dùng được xem: quản lý / duyệt vị trí (người phỏng vấn không thấy lương đề xuất). */
    public Page<Offer> list(Actor actor, String status, UUID storeId, Pageable pageable) {
        MapSqlParameterSource params = RecruitmentAccess.params(actor);
        StringBuilder where = new StringBuilder(" where ").append(RecruitmentAccess.positionVisible("p", "c"));
        if (StringUtils.hasText(status)) { where.append(" and o.status = :status"); params.addValue("status", status.trim().toUpperCase()); }
        if (storeId != null) { where.append(" and c.store_id = :filterStore"); params.addValue("filterStore", storeId); }
        Long total = jdbc.queryForObject("select count(*) from recruitment_offers o join job_applications a on a.id = o.application_id"
                + " join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id" + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<Offer> rows = query(actor, where + " order by case o.status when 'PENDING_APPROVAL' then 0 when 'APPROVED' then 1 when 'SENT' then 2 else 3 end,"
                + " o.created_at desc limit :limit offset :offset", params);
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    record OfferState(UUID id, UUID applicationId, UUID positionId, String status, LocalDate startDate) {
    }

    private OfferState lock(UUID offerId) {
        List<OfferState> rows = jdbc.query("""
                select o.id, o.application_id, a.position_id, o.status, o.start_date from recruitment_offers o
                join job_applications a on a.id = o.application_id where o.id = :id for update of o""",
                new MapSqlParameterSource("id", offerId), (rs, i) -> new OfferState(rs.getObject("id", UUID.class),
                        rs.getObject("application_id", UUID.class), rs.getObject("position_id", UUID.class), rs.getString("status"),
                        rs.getObject("start_date", LocalDate.class)));
        if (rows.isEmpty()) throw BusinessException.notFound("Offer không tồn tại");
        return rows.getFirst();
    }

    private Offer get(Actor actor, UUID id) {
        return query(actor, " where o.id = :id", new MapSqlParameterSource("id", id)).getFirst();
    }

    // ------------------------------------------------------------------ ghi

    @Transactional
    public Offer create(Actor actor, UUID applicationId, OfferRequest request) {
        access.requireManageApplication(actor, applicationId);
        AppState app = workflow.lock(applicationId);
        if (!OFFERABLE.contains(app.status()))
            throw BusinessException.invalidState("Hồ sơ đang \"" + app.status().label() + "\", chưa thể tạo offer");
        validate(request);
        boolean submit = request.submitForApproval() == null || request.submitForApproval();
        UUID id;
        try {
            id = jdbc.queryForObject("""
                    insert into recruitment_offers (application_id, job_title, salary, start_date, response_deadline, note, status, created_by)
                    values (:a, :title, :salary, :start, :deadline, :note, :status, :actor) returning id""",
                    params(request).addValue("a", applicationId).addValue("status", submit ? "PENDING_APPROVAL" : "DRAFT")
                            .addValue("actor", actor.userId()), UUID.class);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw BusinessException.conflict("Hồ sơ đã có một offer đang hiệu lực");
        }
        workflow.history(applicationId, "OFFER", null, null, submit ? "PENDING_APPROVAL" : "DRAFT",
                "Tạo offer " + money(request.salary()) + ", bắt đầu " + request.startDate(), null, actor.userId());
        if (app.status() != ApplicationStatus.OFFER_PENDING)
            workflow.setStatus(app, ApplicationStatus.OFFER_PENDING, actor.userId(), null, null);
        if (submit) notifyApprovers(actor, applicationId, app.positionId());
        return get(actor, id);
    }

    @Transactional
    public Offer update(Actor actor, UUID id, OfferRequest request) {
        OfferState offer = lock(id);
        access.requireManagePosition(actor, offer.positionId());
        if (!List.of("DRAFT", "PENDING_APPROVAL").contains(offer.status()))
            throw BusinessException.invalidState("Chỉ sửa được offer chưa duyệt");
        validate(request);
        jdbc.update("""
                update recruitment_offers set job_title = :title, salary = :salary, start_date = :start,
                    response_deadline = :deadline, note = :note, updated_at = now() at time zone 'UTC' where id = :id""",
                params(request).addValue("id", id));
        workflow.history(offer.applicationId(), "OFFER", null, null, offer.status(), "Cập nhật offer " + money(request.salary()), null, actor.userId());
        return get(actor, id);
    }

    @Transactional
    public Offer submit(Actor actor, UUID id) {
        OfferState offer = lock(id);
        access.requireManagePosition(actor, offer.positionId());
        transition(offer, "DRAFT", "PENDING_APPROVAL", "Gửi duyệt offer", null, actor);
        notifyApprovers(actor, offer.applicationId(), offer.positionId());
        return get(actor, id);
    }

    @Transactional
    public Offer approve(Actor actor, UUID id) {
        OfferState offer = lock(id);
        requireApprove(actor, offer);
        jdbc.update("update recruitment_offers set approved_by = :by, approved_at = now() at time zone 'UTC' where id = :id",
                new MapSqlParameterSource("id", id).addValue("by", actor.userId()));
        transition(offer, "PENDING_APPROVAL", "APPROVED", "Duyệt offer", null, actor);
        return get(actor, id);
    }

    @Transactional
    public Offer rejectApproval(Actor actor, UUID id, String reason) {
        OfferState offer = lock(id);
        requireApprove(actor, offer);
        if (!StringUtils.hasText(reason)) throw BusinessException.badRequest("Vui lòng nhập lý do trả lại");
        transition(offer, "PENDING_APPROVAL", "DRAFT", "Trả lại offer để chỉnh sửa", reason, actor);
        return get(actor, id);
    }

    @Transactional
    public Offer send(Actor actor, UUID id, boolean sendEmail) {
        OfferState offer = lock(id);
        access.requireManagePosition(actor, offer.positionId());
        transition(offer, "APPROVED", "SENT", sendEmail ? "Gửi offer cho ứng viên qua email" : "Đánh dấu đã gửi offer (ngoài hệ thống)", null, actor);
        jdbc.update("update recruitment_offers set sent_at = now() at time zone 'UTC' where id = :id", new MapSqlParameterSource("id", id));
        AppState app = workflow.lock(offer.applicationId());
        workflow.setStatus(app, ApplicationStatus.OFFER_SENT, actor.userId(), null, null);
        Offer sent = get(actor, id);
        if (sendEmail) {
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("jobTitle", sent.jobTitle());
            vars.put("salary", money(sent.salary()));
            vars.put("startDate", DAY.format(sent.startDate()));
            vars.put("responseDeadline", DAY.format(sent.responseDeadline()));
            vars.put("offerNote", StringUtils.hasText(sent.note()) ? "Ghi chú: " + sent.note() : "");
            if (!mailer.sendTemplate(offer.applicationId(), RecruitmentMailer.OFFER_LETTER, vars, actor.userId()))
                throw BusinessException.badRequest("Không gửi được offer qua email (ứng viên chưa có email hoặc mẫu email đang tắt). "
                        + "Chọn \"Đánh dấu đã gửi\" nếu đã gửi bằng cách khác.");
        }
        return sent;
    }

    /** Ghi nhận phản hồi của ứng viên (HR nhập lại sau khi ứng viên trả lời). */
    @Transactional
    public Offer respond(Actor actor, UUID id, boolean accepted, String reason) {
        OfferState offer = lock(id);
        access.requireManagePosition(actor, offer.positionId());
        if (!accepted && !StringUtils.hasText(reason)) throw BusinessException.badRequest("Vui lòng nhập lý do ứng viên từ chối");
        transition(offer, "SENT", accepted ? "ACCEPTED" : "DECLINED",
                accepted ? "Ứng viên chấp nhận offer" : "Ứng viên từ chối offer", reason, actor);
        jdbc.update("update recruitment_offers set responded_at = now() at time zone 'UTC', decline_reason = :reason where id = :id",
                new MapSqlParameterSource("id", id).addValue("reason", accepted ? null : trim(reason)));
        AppState app = workflow.lock(offer.applicationId());
        closeOfferStage(app, accepted ? StageStatus.PASSED : StageStatus.FAILED, actor.userId(), reason);
        workflow.setStatus(app, accepted ? ApplicationStatus.OFFER_ACCEPTED : ApplicationStatus.OFFER_DECLINED,
                actor.userId(), null, accepted ? null : reason);
        return get(actor, id);
    }

    @Transactional
    public Offer cancel(Actor actor, UUID id, String reason) {
        OfferState offer = lock(id);
        access.requireManagePosition(actor, offer.positionId());
        if (!List.of("DRAFT", "PENDING_APPROVAL", "APPROVED", "SENT").contains(offer.status()))
            throw BusinessException.invalidState("Offer đã kết thúc");
        if (!StringUtils.hasText(reason)) throw BusinessException.badRequest("Vui lòng nhập lý do hủy");
        transition(offer, offer.status(), "CANCELLED", "Hủy offer", reason, actor);
        AppState app = workflow.lock(offer.applicationId());
        if (app.status() == ApplicationStatus.OFFER_SENT)
            workflow.setStatus(app, ApplicationStatus.OFFER_PENDING, actor.userId(), null, reason);
        return get(actor, id);
    }

    /** Offer đã gửi quá hạn phản hồi → hết hạn, hồ sơ "Từ chối Offer". Gọi từ scheduler. */
    @Transactional
    public int expireOverdue() {
        List<UUID> overdue = jdbc.queryForList("""
                select id from recruitment_offers where status = 'SENT' and response_deadline < :today for update skip locked""",
                new MapSqlParameterSource("today", LocalDate.now()), UUID.class);
        for (UUID id : overdue) {
            OfferState offer = lock(id);
            jdbc.update("update recruitment_offers set status = 'EXPIRED', updated_at = now() at time zone 'UTC' where id = :id",
                    new MapSqlParameterSource("id", id));
            workflow.history(offer.applicationId(), "OFFER", null, "SENT", "EXPIRED", "Offer hết hạn phản hồi", null, null);
            AppState app = workflow.lock(offer.applicationId());
            if (app.status() == ApplicationStatus.OFFER_SENT) {
                closeOfferStage(app, StageStatus.FAILED, null, "Hết hạn phản hồi offer");
                workflow.setStatus(app, ApplicationStatus.OFFER_DECLINED, null, null, "Hết hạn phản hồi offer");
            }
        }
        return overdue.size();
    }

    /**
     * Chuyển ứng viên đã nhận offer sang module Nhân sự: tạo tài khoản nhân viên (qua EmployeeAdministrationService, nên
     * người thao tác cần quyền tạo nhân viên trong phạm vi của mình), ngày vào làm = ngày bắt đầu của offer.
     */
    @Transactional
    public HireResult hire(Actor actor, UUID applicationId, HireRequest request) {
        access.requireManageApplication(actor, applicationId);
        AppState app = workflow.lock(applicationId);
        if (app.status() != ApplicationStatus.OFFER_ACCEPTED)
            throw BusinessException.invalidState("Chỉ chuyển thành nhân viên khi ứng viên đã chấp nhận offer");
        record Info(UUID candidateId, UUID existingEmployee, String fullName, String jobTitle, LocalDate startDate,
                    UUID campaignStore, LocalDate dateOfBirth, String gender, String workplace, UUID hiringManager) {
        }
        Info info = jdbc.query("""
                select cd.id as candidate_id, cd.employee_id, cd.full_name, o.job_title, o.start_date, c.store_id,
                       cd.date_of_birth, cd.gender, coalesce(p.work_location, st.name) as workplace, p.hiring_manager_id
                from job_applications a
                join candidates cd on cd.id = a.candidate_id
                join recruitment_positions p on p.id = a.position_id join recruitment_campaigns c on c.id = p.campaign_id
                left join stores st on st.id = c.store_id
                join recruitment_offers o on o.application_id = a.id and o.status = 'ACCEPTED'
                where a.id = :a""", new MapSqlParameterSource("a", applicationId), (rs, i) -> new Info(
                rs.getObject("candidate_id", UUID.class), rs.getObject("employee_id", UUID.class), rs.getString("full_name"),
                rs.getString("job_title"), rs.getObject("start_date", LocalDate.class), rs.getObject("store_id", UUID.class),
                rs.getObject("date_of_birth", LocalDate.class), rs.getString("gender"), rs.getString("workplace"),
                rs.getObject("hiring_manager_id", UUID.class))).getFirst();
        if (info.existingEmployee() != null && Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from users where id = :id and deleted_at is null)",
                new MapSqlParameterSource("id", info.existingEmployee()), Boolean.class)))
            throw BusinessException.conflict("Ứng viên này đã là nhân viên");
        EmploymentType type;
        try {
            type = EmploymentType.valueOf(request.employmentType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("Loại hợp đồng không hợp lệ");
        }
        UUID store = request.storeId() != null ? request.storeId() : info.campaignStore();
        CreateEmployeeResponse created = employees.create(actor.userId(), new CreateEmployeeRequest(
                trim(request.username()), request.email().trim(), info.fullName(), request.phone().trim(), info.jobTitle(),
                type, request.departmentId(), request.orgPositionId(), request.roleCodes(), store));
        UUID employeeId = created.employee().id();
        // Chuyển thông tin hồ sơ ứng viên sang hồ sơ nhân viên.
        List<String> transferred = new java.util.ArrayList<>(List.of("Họ tên", "Email", "Số điện thoại", "Chức danh", "Ngày vào làm (theo offer)"));
        String gender = info.gender() != null && List.of("MALE", "FEMALE", "OTHER").contains(info.gender()) ? info.gender() : null;
        UUID manager = info.hiringManager() != null && !info.hiringManager().equals(employeeId) ? info.hiringManager() : null;
        jdbc.update("""
                update users set hire_date = :start, date_of_birth = coalesce(date_of_birth, :dob),
                    gender = coalesce(gender, :gender), work_location = coalesce(work_location, :workplace),
                    manager_id = coalesce(manager_id, :manager)
                where id = :id""",
                new MapSqlParameterSource("start", info.startDate()).addValue("id", employeeId).addValue("dob", info.dateOfBirth())
                        .addValue("gender", gender).addValue("workplace", info.workplace()).addValue("manager", manager));
        if (info.dateOfBirth() != null) transferred.add("Ngày sinh");
        if (gender != null) transferred.add("Giới tính");
        if (info.workplace() != null) transferred.add("Nơi làm việc");
        if (manager != null) transferred.add("Quản lý trực tiếp (hiring manager)");
        jdbc.update("update candidates set employee_id = :e, updated_at = now() at time zone 'UTC' where id = :c",
                new MapSqlParameterSource("e", employeeId).addValue("c", info.candidateId()));
        jdbc.update("""
                update job_applications set hired_employee_id = :e, hired_at = now() at time zone 'UTC' where id = :a""",
                new MapSqlParameterSource("e", employeeId).addValue("a", applicationId));
        workflow.setStatus(app, ApplicationStatus.HIRED, actor.userId(),
                "Đã tạo nhân viên " + created.employee().employeeCode() + " (" + created.employee().username() + ")", null);
        // Đủ chỉ tiêu → vị trí "Đã tuyển đủ".
        jdbc.update("""
                update recruitment_positions p set status = 'FILLED', updated_at = now() at time zone 'UTC'
                where p.id = :p and p.status <> 'FILLED'
                  and (select count(*) from job_applications a where a.position_id = p.id and a.status = 'HIRED') >= p.headcount""",
                new MapSqlParameterSource("p", app.positionId()));
        workflow.history(applicationId, "NOTE", null, null, null, "Chuyển sang hồ sơ nhân viên: " + String.join(", ", transferred)
                + ". CV và tài liệu vẫn lưu trong hồ sơ ứng viên.", null, actor.userId());
        boolean welcome = false;
        if (!Boolean.FALSE.equals(request.sendWelcomeEmail())) {
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("jobTitle", info.jobTitle());
            vars.put("startDate", DAY.format(info.startDate()));
            vars.put("employeeCode", created.employee().employeeCode());
            vars.put("username", created.employee().username());
            welcome = mailer.sendTemplate(applicationId, RecruitmentMailer.ONBOARDING, vars, actor.userId());
        }
        return new HireResult(employeeId, created.employee().employeeCode(), created.employee().username(),
                created.temporaryPassword(), transferred, welcome);
    }

    // ------------------------------------------------------------------ nội bộ

    private void transition(OfferState offer, String from, String to, String message, String reason, Actor actor) {
        if (!from.equals(offer.status())) throw BusinessException.invalidState("Offer không ở trạng thái phù hợp (" + offer.status() + ")");
        jdbc.update("update recruitment_offers set status = :to, updated_at = now() at time zone 'UTC' where id = :id",
                new MapSqlParameterSource("id", offer.id()).addValue("to", to));
        workflow.history(offer.applicationId(), "OFFER", null, from, to, message, reason, actor.userId());
    }

    /** Vòng Offer (nếu hồ sơ đang ở đó) nhận kết quả theo phản hồi của ứng viên, không đụng tới trạng thái tổng thể. */
    private void closeOfferStage(AppState app, StageStatus outcome, UUID actor, String note) {
        if (app.currentStageId() == null) return;
        jdbc.update("""
                update application_stage_results r set status = :status, decided_by = :by, decided_at = now() at time zone 'UTC',
                    decision_note = :note
                from recruitment_stages s where s.id = r.stage_id and s.stage_type = 'OFFER'
                  and r.application_id = :a and r.stage_id = :s""",
                new MapSqlParameterSource("status", outcome.name()).addValue("by", actor).addValue("note", trim(note))
                        .addValue("a", app.id()).addValue("s", app.currentStageId()));
    }

    private void requireApprove(Actor actor, OfferState offer) {
        if (!access.canApprovePosition(actor, offer.positionId()))
            throw BusinessException.forbidden("Bạn không có quyền duyệt offer của vị trí này");
    }

    private void notifyApprovers(Actor actor, UUID applicationId, UUID positionId) {
        List<UUID> approvers = jdbc.queryForList("""
                select p.hiring_manager_id from recruitment_positions p where p.id = :p and p.hiring_manager_id is not null
                union select m.user_id from recruitment_campaign_members m join recruitment_positions p on p.campaign_id = m.campaign_id
                where p.id = :p and m.member_role = 'HIRING_MANAGER'""", new MapSqlParameterSource("p", positionId), UUID.class);
        String what = jdbc.queryForObject("""
                select cd.full_name || ' – ' || p.title from job_applications a join candidates cd on cd.id = a.candidate_id
                join recruitment_positions p on p.id = a.position_id where a.id = :a""", new MapSqlParameterSource("a", applicationId), String.class);
        notifier.notifyUsers(approvers, actor.userId(), "Offer chờ bạn duyệt", what, applicationId);
    }

    private static void validate(OfferRequest r) {
        if (r.responseDeadline().isBefore(LocalDate.now()))
            throw BusinessException.badRequest("Hạn phản hồi phải từ hôm nay trở đi");
        if (r.startDate().isBefore(r.responseDeadline()))
            throw BusinessException.badRequest("Ngày bắt đầu làm việc phải sau hạn phản hồi");
    }

    private static MapSqlParameterSource params(OfferRequest r) {
        return new MapSqlParameterSource("title", r.jobTitle().trim()).addValue("salary", r.salary())
                .addValue("start", r.startDate()).addValue("deadline", r.responseDeadline()).addValue("note", trim(r.note()));
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ";
    }
}
