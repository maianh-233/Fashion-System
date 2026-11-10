package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.service.StoreAccessService;
import com.fashionsystem.fashion_system.service.UserScope;
import com.fashionsystem.fashion_system.service.UserScopeService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Toàn bộ luật ai xem / quản lý / duyệt được chiến dịch. Quyền hiệu lực = mã quyền (RBAC) và phạm vi nhân viên:
 * <ul>
 *   <li>nhân viên cấp chuỗi (Global) có MANAGE quản lý mọi chiến dịch;</li>
 *   <li>nhân viên cửa hàng có MANAGE chỉ quản lý chiến dịch của cửa hàng mình ({@code store_id} = cửa hàng của họ);</li>
 *   <li>duyệt luôn là cấp chuỗi;</li>
 *   <li>nhân viên cửa hàng chỉ có VIEW thấy chiến dịch đã duyệt của toàn chuỗi và của cửa hàng mình, không thấy số liệu tài chính.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
class MarketingCampaignAccess {
    static final String VIEW = "MARKETING_CAMPAIGN_VIEW";
    static final String MANAGE = "MARKETING_CAMPAIGN_MANAGE";
    static final String APPROVE = "MARKETING_CAMPAIGN_APPROVE";
    static final String PROMOTION_VIEW = "PROMOTION_VIEW";

    private final NamedParameterJdbcTemplate jdbc;
    private final StoreAccessService storeAccess;
    private final UserScopeService scopes;

    boolean isGlobal(UUID actor) {
        return scopes.resolve(actor).isGlobal();
    }

    /** Cửa hàng của nhân viên; null với nhân viên cấp chuỗi. */
    UUID ownStore(UUID actor) {
        UserScope scope = scopes.resolve(actor);
        return scope.isGlobal() ? null : scope.storeId();
    }

    /** Quản lý cấp chuỗi: có quyền và là nhân viên Global (allowsScope toàn hệ thống chỉ đúng cho Global). */
    boolean canManage(UUID actor) {
        return storeAccess.allowsScope(actor, null, MANAGE);
    }

    /** Quản lý được chiến dịch thuộc phạm vi {@code campaignStore} (null = chiến dịch toàn chuỗi). */
    boolean canManageCampaign(UUID actor, UUID campaignStore) {
        if (canManage(actor)) return true;
        return campaignStore != null && storeAccess.allows(actor, campaignStore, MANAGE);
    }

    /** Tạo được chiến dịch: quản lý cấp chuỗi, hoặc nhân viên cửa hàng có MANAGE (cho cửa hàng mình). */
    boolean canCreate(UUID actor) {
        if (canManage(actor)) return true;
        UUID store = ownStore(actor);
        return store != null && storeAccess.allows(actor, store, MANAGE);
    }

    boolean canApprove(UUID actor) {
        return storeAccess.allowsScope(actor, null, APPROVE);
    }

    boolean canLinkOffers(UUID actor, UUID campaignStore) {
        if (!canManageCampaign(actor, campaignStore)) return false;
        return isGlobal(actor) ? storeAccess.allowsScope(actor, null, PROMOTION_VIEW)
                : storeAccess.allows(actor, campaignStore, PROMOTION_VIEW);
    }

    /** Số liệu tài chính, lịch sử, đợt gửi, báo cáo: nhân viên cấp chuỗi hoặc người quản lý được chiến dịch đó. */
    boolean canViewFinancials(UUID actor, UUID campaignStore) {
        return isGlobal(actor) || canManageCampaign(actor, campaignStore);
    }

    /** Nhân viên cửa hàng thấy chiến dịch: đã duyệt (toàn chuỗi hoặc cửa hàng mình), hoặc mọi trạng thái nếu quản lý được. */
    boolean canSee(UUID actor, UUID campaignStore, String status) {
        UUID own = ownStore(actor);
        if (own == null) return true;
        if (campaignStore != null && !campaignStore.equals(own)) return false;
        return MarketingCampaignService.APPROVED.equals(status) || canManageCampaign(actor, campaignStore);
    }

    void requireCreate(UUID actor) {
        if (!canCreate(actor)) throw BusinessException.forbidden("Bạn không có quyền tạo chiến dịch");
    }

    void requireManage(UUID actor) {
        if (!canManage(actor)) throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi có quyền quản lý mới thao tác được chiến dịch");
    }

    void requireManageCampaign(UUID actor, UUID campaignStore) {
        if (!canManageCampaign(actor, campaignStore))
            throw BusinessException.forbidden(campaignStore == null
                    ? "Chỉ nhân viên cấp chuỗi có quyền quản lý mới thao tác được chiến dịch toàn chuỗi"
                    : "Bạn chỉ quản lý được chiến dịch của cửa hàng mình");
    }

    void requireApprove(UUID actor) {
        if (!canApprove(actor)) throw BusinessException.forbidden("Chỉ nhân viên cấp chuỗi có quyền duyệt mới duyệt được chiến dịch");
    }

    void requireLinkOffers(UUID actor, UUID campaignStore) {
        if (!canLinkOffers(actor, campaignStore))
            throw BusinessException.forbidden("Cần quyền quản lý chiến dịch và xem khuyến mãi để liên kết ưu đãi");
    }

    void requireFinancials(UUID actor, UUID campaignStore) {
        if (!canViewFinancials(actor, campaignStore)) throw BusinessException.forbidden("Bạn không xem được số liệu của chiến dịch này");
    }

    boolean isSuperAdmin(UUID actor) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from user_roles ur join roles r on r.id = ur.role_id
                               where ur.user_id = :actor and r.code = 'SUPER_ADMIN')""",
                new MapSqlParameterSource("actor", actor), Boolean.class));
    }

    /** Người được báo khi có chiến dịch chờ duyệt: nhân viên cấp chuỗi đang hoạt động có quyền duyệt (trừ người gửi). */
    List<UUID> approvers(UUID except) {
        List<UUID> candidates = jdbc.queryForList("""
                select distinct u.id from users u
                where u.deleted_at is null and coalesce(u.active, false) and not coalesce(u.locked, false)
                  and u.id <> :except
                  and (exists (select 1 from user_roles ur join role_permissions rp on rp.role_id = ur.role_id
                               join permissions p on p.id = rp.permission_id
                               where ur.user_id = u.id and p.code = :permission)
                       or exists (select 1 from user_permissions up join permissions p on p.id = up.permission_id
                                  where up.user_id = u.id and p.code = :permission and up.effect = 'ALLOW'))
                """, new MapSqlParameterSource("except", except).addValue("permission", APPROVE), UUID.class);
        return candidates.stream().filter(id -> {
            try {
                return canApprove(id);
            } catch (BusinessException ambiguousScope) {
                return false;
            }
        }).toList();
    }

    private static final String ACTIVE_USER = "u.deleted_at is null and coalesce(u.active, false) and not coalesce(u.locked, false)";

    /** Nhân viên cấp chuỗi đang hoạt động: người phụ trách chiến dịch toàn chuỗi phải thuộc nhóm này. */
    List<MarketingCampaignDtos.Ref> globalEmployees() {
        return jdbc.query("select u.id, coalesce(u.full_name, u.username) as name from users u where " + ACTIVE_USER
                        + " and not exists (select 1 from store_staffs ss where ss.user_id = u.id and ss.active) order by 2",
                new MapSqlParameterSource(), (rs, i) -> new MarketingCampaignDtos.Ref(rs.getObject("id", UUID.class), rs.getString("name")));
    }

    /** Nhân viên đang hoạt động của một cửa hàng. */
    List<MarketingCampaignDtos.Ref> storeEmployees(UUID store) {
        return jdbc.query("select u.id, coalesce(u.full_name, u.username) as name from users u where " + ACTIVE_USER
                        + " and exists (select 1 from store_staffs ss where ss.user_id = u.id and ss.active and ss.store_id = :store) order by 2",
                new MapSqlParameterSource("store", store),
                (rs, i) -> new MarketingCampaignDtos.Ref(rs.getObject("id", UUID.class), rs.getString("name")));
    }

    /**
     * Người phụ trách hợp lệ: nhân viên đang hoạt động thuộc cấp chuỗi, hoặc (với chiến dịch cửa hàng) thuộc chính cửa
     * hàng đó.
     */
    boolean isValidOwner(UUID user, UUID campaignStore) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists (select 1 from users u where u.id = :id and " + ACTIVE_USER
                        + " and (not exists (select 1 from store_staffs ss where ss.user_id = u.id and ss.active)"
                        + " or exists (select 1 from store_staffs ss where ss.user_id = u.id and ss.active"
                        + " and ss.store_id = cast(:store as uuid))))",
                new MapSqlParameterSource("id", user).addValue("store", campaignStore, java.sql.Types.OTHER), Boolean.class));
    }
}
