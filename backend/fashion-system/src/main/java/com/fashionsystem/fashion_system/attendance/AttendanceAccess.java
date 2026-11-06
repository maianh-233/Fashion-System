package com.fashionsystem.fashion_system.attendance;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.UserScope;
import com.fashionsystem.fashion_system.service.UserScopeService;
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
 * Phân quyền dữ liệu chấm công (kiểm tra ở backend). Phạm vi lấy từ phân công cửa hàng ({@link UserScopeService}):
 * nhân viên cấp chuỗi (HR Global / Central) thấy mọi cửa hàng và nhân viên Global, có thể lọc theo cửa hàng;
 * nhân viên cửa hàng (HR Store, quản lý cửa hàng) chỉ thấy nhân viên đang thuộc cửa hàng mình và bản ghi phát sinh
 * tại cửa hàng mình (kể cả nhân viên Global đến làm tại đó).
 */
@Component
public class AttendanceAccess {
    private final UserScopeService scopes;
    private final NamedParameterJdbcTemplate jdbc;

    public AttendanceAccess(UserScopeService scopes, NamedParameterJdbcTemplate jdbc) {
        this.scopes = scopes;
        this.jdbc = jdbc;
    }

    /**
     * @param storeId null = cấp chuỗi
     */
    public record Actor(UUID userId, UUID storeId, String storeName, boolean view, boolean manage, boolean approveLeave,
                        boolean export, boolean admin) {
        public boolean global() {
            return storeId == null;
        }

        /** Cấu hình dùng chung toàn chuỗi (giờ làm mặc định, ngày lễ, loại nghỉ). */
        public boolean globalAdmin() {
            return admin && storeId == null;
        }

        public AttendanceDtos.Permissions permissions() {
            return new AttendanceDtos.Permissions(view || admin, manage || admin, approveLeave || admin, export || admin, admin,
                    globalAdmin());
        }
    }

    /** Người đang đăng nhập (phải là nhân viên). */
    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user))
            throw BusinessException.unauthorized("Chưa đăng nhập bằng tài khoản nhân viên");
        return user.userId();
    }

    public Actor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = currentUserId();
        Set<String> codes = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        boolean admin = codes.contains("ATTENDANCE_ADMIN");
        boolean view = codes.contains("ATTENDANCE_VIEW") || codes.contains("LEAVE_VIEW") || codes.contains("SHIFT_VIEW")
                || codes.contains("SHIFT_MANAGE");
        if (!view && !admin) throw BusinessException.forbidden("Bạn không có quyền truy cập dữ liệu chấm công");
        UserScope scope = scopes.resolve(userId);
        return new Actor(userId, scope.isGlobal() ? null : scope.storeId(), scope.isGlobal() ? null : scope.storeName(),
                view, codes.contains("ATTENDANCE_MANAGE"), codes.contains("LEAVE_APPROVE"), codes.contains("ATTENDANCE_EXPORT"),
                admin);
    }

    /** Cửa hàng hiện tại của nhân viên (null = Global). */
    public UUID storeOf(UUID userId) {
        UserScope scope = scopes.resolve(userId);
        return scope.isGlobal() ? null : scope.storeId();
    }

    /** Bộ lọc cửa hàng: nhân viên cửa hàng luôn bị ép về cửa hàng mình, chọn cửa hàng khác bị từ chối. */
    public UUID storeFilter(Actor actor, UUID requested) {
        if (actor.global()) return requested;
        if (requested != null && !requested.equals(actor.storeId()))
            throw BusinessException.forbidden("Bạn không có quyền xem dữ liệu cửa hàng này");
        return actor.storeId();
    }

    // ------------------------------------------------------------------ điều kiện SQL
    // Tham số: :global, :scopeStore (cửa hàng của người xem), :store (bộ lọc), :employeeType (STORE | GLOBAL | null).

    /**
     * Dòng có nhân viên {@code userCol} và cửa hàng phát sinh {@code storeCol} nằm trong phạm vi người xem và khớp bộ lọc.
     * Nhân viên "thuộc cửa hàng" = có phân công cửa hàng đang hiệu lực.
     */
    static String guard(String userCol, String storeCol) {
        String inStore = "exists (select 1 from store_staffs gs where gs.user_id = " + userCol + " and gs.active and gs.store_id = %s)";
        String isGlobalEmployee = "not exists (select 1 from store_staffs gs where gs.user_id = " + userCol + " and gs.active)";
        return "((:global or " + inStore.formatted(":scopeStore") + " or " + storeCol + " = :scopeStore)"
                + " and (cast(:store as uuid) is null or " + inStore.formatted(":store") + " or " + storeCol + " = :store)"
                + " and (cast(:employeeType as varchar) is null"
                + " or (:employeeType = 'GLOBAL' and " + isGlobalEmployee + ")"
                + " or (:employeeType = 'STORE' and not " + isGlobalEmployee + ")))";
    }

    static MapSqlParameterSource params(Actor actor, UUID store, String employeeType) {
        return new MapSqlParameterSource("global", actor.global())
                .addValue("scopeStore", actor.storeId(), java.sql.Types.OTHER)
                .addValue("store", store, java.sql.Types.OTHER)
                .addValue("employeeType", employeeType, java.sql.Types.VARCHAR);
    }

    /** Nhân viên {@code targetUserId} (với bản ghi phát sinh tại {@code recordStoreId}) nằm trong phạm vi người xem. */
    public boolean canSee(Actor actor, UUID targetUserId, UUID recordStoreId) {
        if (actor.global()) return true;
        if (actor.storeId().equals(recordStoreId)) return true;
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from store_staffs where user_id = :u and active and store_id = :s)",
                new MapSqlParameterSource("u", targetUserId).addValue("s", actor.storeId()), Boolean.class));
    }

    public void requireSee(Actor actor, UUID targetUserId, UUID recordStoreId) {
        if (!canSee(actor, targetUserId, recordStoreId))
            throw BusinessException.forbidden("Nhân viên này không thuộc phạm vi quản lý của bạn");
    }

    /** Duyệt đơn / điều chỉnh công: có quyền, trong phạm vi và không phải dữ liệu của chính mình. */
    public void requireDecide(Actor actor, boolean permitted, UUID targetUserId, UUID recordStoreId) {
        if (!permitted && !actor.admin()) throw BusinessException.forbidden("Bạn không có quyền xử lý yêu cầu này");
        requireSee(actor, targetUserId, recordStoreId);
        if (actor.userId().equals(targetUserId))
            throw BusinessException.forbidden("Không thể tự duyệt hoặc điều chỉnh dữ liệu chấm công của chính mình");
    }
}
