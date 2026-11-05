package com.fashionsystem.fashion_system.recruitment;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.UserScope;
import com.fashionsystem.fashion_system.service.UserScopeService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Phân quyền dữ liệu tuyển dụng (kiểm tra ở backend, frontend chỉ ẩn / hiện). Hai lớp:
 *
 * <p><b>Phạm vi cửa hàng.</b> Mỗi chiến dịch thuộc một cửa hàng hoặc văn phòng / toàn chuỗi (store null). Nhân viên cấp
 * chuỗi (không gán cửa hàng) có thể thấy mọi cửa hàng; nhân viên cửa hàng không bao giờ thấy chiến dịch của cửa hàng
 * khác, chỉ thấy chiến dịch cửa hàng mình và chiến dịch chuỗi mà họ được phân công.
 *
 * <p><b>Vai trò.</b>
 * <ul>
 *   <li>RECRUITMENT_ADMIN (HR Admin; Quản lý cửa hàng): toàn quyền trong phạm vi của mình (cấp chuỗi = mọi nơi).</li>
 *   <li>RECRUITMENT_MANAGE (HR Recruiter): chiến dịch mình phụ trách / được thêm làm RECRUITER.</li>
 *   <li>RECRUITMENT_EVALUATE (Interviewer): chỉ hồ sơ được phân công đánh giá / phỏng vấn.</li>
 *   <li>RECRUITMENT_APPROVE (Hiring Manager): vị trí mình là hiring manager hoặc chiến dịch có mình là HIRING_MANAGER.</li>
 * </ul>
 */
@Component
public class RecruitmentAccess {
    private final NamedParameterJdbcTemplate jdbc;
    private final UserScopeService scopes;

    public RecruitmentAccess(NamedParameterJdbcTemplate jdbc, UserScopeService scopes) {
        this.jdbc = jdbc;
        this.scopes = scopes;
    }

    /**
     * @param admin   có RECRUITMENT_ADMIN (trong phạm vi {@code storeId})
     * @param storeId null = nhân viên cấp chuỗi
     */
    public record Actor(UUID userId, boolean admin, boolean manage, boolean evaluate, boolean approve, UUID storeId,
                        String storeName) {
        public boolean global() {
            return storeId == null;
        }

        /** Quản trị toàn chuỗi (cấu hình dùng chung như mẫu quy trình). */
        public boolean globalAdmin() {
            return admin && storeId == null;
        }

        /** Có quyền quản trị với dữ liệu thuộc cửa hàng {@code ownerStoreId} (null = văn phòng). */
        public boolean adminOf(UUID ownerStoreId) {
            return admin && (storeId == null || storeId.equals(ownerStoreId));
        }

        /** Dữ liệu thuộc {@code ownerStoreId} có nằm trong phạm vi cửa hàng của người dùng không. */
        public boolean inScope(UUID ownerStoreId) {
            return storeId == null || ownerStoreId == null || storeId.equals(ownerStoreId);
        }

        public RecruitmentDtos.Permissions permissions() {
            return new RecruitmentDtos.Permissions(admin, admin || manage, admin || evaluate, admin || approve);
        }
    }

    public Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user))
            throw BusinessException.unauthorized("Chưa đăng nhập");
        Set<String> codes = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        if (!codes.contains("RECRUITMENT_VIEW") && !codes.contains("RECRUITMENT_ADMIN"))
            throw BusinessException.forbidden("Bạn không có quyền truy cập module tuyển dụng");
        UserScope scope = scopes.resolve(user.userId());
        return new Actor(user.userId(), codes.contains("RECRUITMENT_ADMIN"), codes.contains("RECRUITMENT_MANAGE"),
                codes.contains("RECRUITMENT_EVALUATE"), codes.contains("RECRUITMENT_APPROVE"),
                scope.isGlobal() ? null : scope.storeId(), scope.isGlobal() ? null : scope.storeName());
    }

    // ------------------------------------------------------------------ điều kiện SQL
    // Tham số: :actor, :global (cấp chuỗi), :store, :globalAdmin, :storeAdmin.

    /** Chiến dịch {@code c} nằm trong phạm vi cửa hàng của người dùng. */
    static String storeGuard(String c) {
        return "(:global or " + c + ".store_id is null or " + c + ".store_id = :store)";
    }

    /** Có quyền quản trị trên chiến dịch {@code c}. */
    static String adminOf(String c) {
        return "(:globalAdmin or (:storeAdmin and " + c + ".store_id = :store))";
    }

    /** Chiến dịch {@code c} người dùng được xem. */
    static String campaignVisible(String c) {
        return "(" + storeGuard(c) + " and (" + adminOf(c) + " or " + c + ".owner_id = :actor"
                + " or exists (select 1 from recruitment_campaign_members vm where vm.campaign_id = " + c + ".id and vm.user_id = :actor)"
                + " or exists (select 1 from recruitment_positions vp where vp.campaign_id = " + c + ".id and vp.hiring_manager_id = :actor)))";
    }

    /** Vị trí {@code p} (thuộc chiến dịch {@code c}) người dùng được xem. */
    static String positionVisible(String p, String c) {
        return "(" + campaignVisible(c) + " or (" + storeGuard(c) + " and (" + p + ".hiring_manager_id = :actor"
                + " or exists (select 1 from recruitment_stages vs where vs.position_id = " + p + ".id and vs.owner_id = :actor))))";
    }

    /** Hồ sơ {@code a}: thấy cả vị trí, hoặc được phân công đánh giá / phỏng vấn hồ sơ đó. */
    static String applicationVisible(String a, String p, String c) {
        return "(" + positionVisible(p, c) + " or (" + storeGuard(c) + " and ("
                + "exists (select 1 from application_stage_evaluators ve where ve.application_id = " + a + ".id and ve.user_id = :actor)"
                + " or exists (select 1 from recruitment_interviews vi join recruitment_interview_participants vip on vip.interview_id = vi.id"
                + " where vi.application_id = " + a + ".id and vip.user_id = :actor))))";
    }

    static MapSqlParameterSource params(Actor actor) {
        return new MapSqlParameterSource("actor", actor.userId())
                .addValue("global", actor.global())
                .addValue("store", actor.storeId(), java.sql.Types.OTHER)
                .addValue("globalAdmin", actor.globalAdmin())
                .addValue("storeAdmin", actor.admin() && !actor.global())
                .addValue("poolAll", actor.global() && (actor.admin() || actor.manage()))
                .addValue("poolStore", !actor.global() && (actor.admin() || actor.manage()));
    }

    /**
     * Ứng viên {@code cd}: HR cấp chuỗi thấy cả kho ứng viên; HR cửa hàng thấy ứng viên do cửa hàng mình tiếp nhận;
     * mọi người thấy ứng viên của hồ sơ mình được xem.
     */
    static String candidateVisible(String cd) {
        return "(" + cd + ".deleted_at is null and (:poolAll or (:poolStore and " + cd + ".store_id = :store)"
                + " or exists (select 1 from job_applications va join recruitment_positions vp on vp.id = va.position_id"
                + " join recruitment_campaigns vc on vc.id = vp.campaign_id where va.candidate_id = " + cd + ".id and "
                + applicationVisible("va", "vp", "vc") + ")))";
    }

    public boolean canViewCandidate(Actor actor, UUID candidateId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from candidates cd where cd.id = :id and "
                + candidateVisible("cd") + ")", params(actor).addValue("id", candidateId), Boolean.class));
    }

    public void requireViewCandidate(Actor actor, UUID candidateId) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from candidates where id = :id and deleted_at is null)",
                new MapSqlParameterSource("id", candidateId), Boolean.class)))
            throw BusinessException.notFound("Ứng viên không tồn tại");
        if (!canViewCandidate(actor, candidateId)) throw BusinessException.forbidden("Bạn không được xem ứng viên này");
    }

    /** Sửa hồ sơ ứng viên / tải tài liệu: thấy ứng viên và có quyền quản lý tuyển dụng. */
    public void requireManageCandidate(Actor actor, UUID candidateId) {
        requireViewCandidate(actor, candidateId);
        if (!actor.admin() && !actor.manage()) throw BusinessException.forbidden("Bạn chỉ có quyền xem ứng viên");
    }

    // ------------------------------------------------------------------ kiểm tra theo đối tượng

    record CampaignScope(UUID storeId, UUID ownerId) {
    }

    CampaignScope campaignScope(UUID campaignId) {
        List<CampaignScope> rows = jdbc.query("select store_id, owner_id from recruitment_campaigns where id = :id",
                new MapSqlParameterSource("id", campaignId),
                (rs, i) -> new CampaignScope(rs.getObject("store_id", UUID.class), rs.getObject("owner_id", UUID.class)));
        if (rows.isEmpty()) throw BusinessException.notFound("Chiến dịch không tồn tại");
        return rows.getFirst();
    }

    public UUID storeOfCampaign(UUID campaignId) {
        return campaignScope(campaignId).storeId();
    }

    /** Quản lý chiến dịch: quản trị trong phạm vi, hoặc RECRUITMENT_MANAGE + người phụ trách / RECRUITER. */
    public boolean canManageCampaign(Actor actor, UUID campaignId) {
        CampaignScope scope = campaignScope(campaignId);
        if (!actor.inScope(scope.storeId())) return false;
        if (actor.adminOf(scope.storeId())) return true;
        if (!actor.manage()) return false;
        if (actor.userId().equals(scope.ownerId())) return true;
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from recruitment_campaign_members m where m.campaign_id = :id and m.user_id = :actor
                               and m.member_role = 'RECRUITER')""",
                new MapSqlParameterSource("id", campaignId).addValue("actor", actor.userId()), Boolean.class));
    }

    public void requireManageCampaign(Actor actor, UUID campaignId) {
        if (!canManageCampaign(actor, campaignId))
            throw BusinessException.forbidden("Bạn không được phân công quản lý chiến dịch này");
    }

    public UUID campaignOfPosition(UUID positionId) {
        var ids = jdbc.queryForList("select campaign_id from recruitment_positions where id = :id",
                new MapSqlParameterSource("id", positionId), UUID.class);
        if (ids.isEmpty()) throw BusinessException.notFound("Vị trí tuyển dụng không tồn tại");
        return ids.getFirst();
    }

    public UUID positionOfApplication(UUID applicationId) {
        var ids = jdbc.queryForList("select position_id from job_applications where id = :id",
                new MapSqlParameterSource("id", applicationId), UUID.class);
        if (ids.isEmpty()) throw BusinessException.notFound("Hồ sơ ứng tuyển không tồn tại");
        return ids.getFirst();
    }

    public UUID storeOfPosition(UUID positionId) {
        return storeOfCampaign(campaignOfPosition(positionId));
    }

    public boolean canManagePosition(Actor actor, UUID positionId) {
        return canManageCampaign(actor, campaignOfPosition(positionId));
    }

    public void requireManagePosition(Actor actor, UUID positionId) {
        if (!canManagePosition(actor, positionId))
            throw BusinessException.forbidden("Bạn không được phân công quản lý vị trí này");
    }

    public boolean canManageApplication(Actor actor, UUID applicationId) {
        return canManagePosition(actor, positionOfApplication(applicationId));
    }

    public void requireManageApplication(Actor actor, UUID applicationId) {
        if (!canManageApplication(actor, applicationId))
            throw BusinessException.forbidden("Bạn không được phân công quản lý hồ sơ này");
    }

    public boolean canViewPosition(Actor actor, UUID positionId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from recruitment_positions p join recruitment_campaigns c on c.id = p.campaign_id where p.id = :id and "
                + positionVisible("p", "c") + ")", params(actor).addValue("id", positionId), Boolean.class));
    }

    public void requireViewPosition(Actor actor, UUID positionId) {
        campaignOfPosition(positionId);
        if (!canViewPosition(actor, positionId)) throw BusinessException.forbidden("Bạn không được xem vị trí này");
    }

    public boolean canViewApplication(Actor actor, UUID applicationId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from job_applications a join recruitment_positions p on p.id = a.position_id
                    join recruitment_campaigns c on c.id = p.campaign_id where a.id = :id and """
                + " " + applicationVisible("a", "p", "c") + ")", params(actor).addValue("id", applicationId), Boolean.class));
    }

    public void requireViewApplication(Actor actor, UUID applicationId) {
        positionOfApplication(applicationId);
        if (!canViewApplication(actor, applicationId))
            throw BusinessException.forbidden("Bạn không được phân công xem hồ sơ này");
    }

    /**
     * Phê duyệt (offer, kết luận vòng thủ công): quản trị trong phạm vi, hoặc RECRUITMENT_APPROVE và là hiring manager
     * của vị trí / HIRING_MANAGER của chiến dịch / người phụ trách chiến dịch.
     */
    public boolean canApprovePosition(Actor actor, UUID positionId) {
        UUID campaignId = campaignOfPosition(positionId);
        CampaignScope scope = campaignScope(campaignId);
        if (!actor.inScope(scope.storeId())) return false;
        if (actor.adminOf(scope.storeId())) return true;
        if (!actor.approve()) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from recruitment_positions p join recruitment_campaigns c on c.id = p.campaign_id
                    where p.id = :id and (p.hiring_manager_id = :actor or c.owner_id = :actor
                        or exists (select 1 from recruitment_campaign_members m where m.campaign_id = c.id
                                   and m.user_id = :actor and m.member_role = 'HIRING_MANAGER')))""",
                new MapSqlParameterSource("id", positionId).addValue("actor", actor.userId()), Boolean.class));
    }

    /**
     * Chấm phiếu ở một vòng: RECRUITMENT_EVALUATE và được phân công (người đánh giá, người phỏng vấn của vòng, người
     * phụ trách vòng) hoặc là người duyệt / quản lý của vị trí; quản trị trong phạm vi luôn được.
     */
    public boolean canEvaluate(Actor actor, UUID applicationId, UUID stageId) {
        UUID positionId = positionOfApplication(applicationId);
        UUID store = storeOfPosition(positionId);
        if (!actor.inScope(store)) return false;
        if (actor.adminOf(store)) return true;
        if (!actor.evaluate()) return false;
        Boolean assigned = jdbc.queryForObject("""
                select exists (select 1 from application_stage_evaluators e
                               where e.application_id = :app and e.stage_id = :stage and e.user_id = :actor)
                    or exists (select 1 from recruitment_interviews i join recruitment_interview_participants ip on ip.interview_id = i.id
                               where i.application_id = :app and i.stage_id = :stage and ip.user_id = :actor and i.status <> 'CANCELLED')
                    or exists (select 1 from recruitment_stages s where s.id = :stage and s.owner_id = :actor)""",
                new MapSqlParameterSource("actor", actor.userId()).addValue("app", applicationId).addValue("stage", stageId), Boolean.class);
        if (Boolean.TRUE.equals(assigned)) return true;
        return canApprovePosition(actor, positionId) || canManagePosition(actor, positionId);
    }

    /**
     * Người được phân công (thành viên, hiring manager, người phụ trách vòng, người đánh giá, người phỏng vấn) của tuyển
     * dụng cửa hàng {@code storeId} phải là nhân viên cửa hàng đó hoặc nhân viên cấp chuỗi.
     */
    public void requireAssignable(UUID userId, UUID storeId, String what) {
        Boolean ok = jdbc.queryForObject("""
                select exists (select 1 from users u where u.id = :u and u.deleted_at is null and coalesce(u.active, true)
                    and (cast(:store as uuid) is null
                         or exists (select 1 from store_staffs s where s.user_id = u.id and s.store_id = :store and s.active)
                         or not exists (select 1 from store_staffs s where s.user_id = u.id and s.active)))""",
                new MapSqlParameterSource("u", userId).addValue("store", storeId, java.sql.Types.OTHER), Boolean.class);
        if (!Boolean.TRUE.equals(ok))
            throw BusinessException.badRequest(what + " phải là nhân viên của cửa hàng này hoặc nhân viên cấp chuỗi");
    }
}
