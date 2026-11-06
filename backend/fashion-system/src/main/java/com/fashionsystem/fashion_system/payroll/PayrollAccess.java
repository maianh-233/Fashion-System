package com.fashionsystem.fashion_system.payroll;

import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.UserScope;
import com.fashionsystem.fashion_system.service.UserScopeService;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Phân quyền dữ liệu lương (kiểm tra ở backend, giao diện chỉ ẩn / hiện):
 * <ul>
 *   <li>HR cửa hàng (có phân công cửa hàng): chỉ kỳ lương và hồ sơ lương của nhân viên cửa hàng mình.</li>
 *   <li>HR cấp chuỗi (Global / Central): chọn cửa hàng bất kỳ, kỳ lương Văn phòng (nhân viên Global).</li>
 *   <li>Hồ sơ lương đánh dấu bảo mật (vd ban giám đốc) chỉ người có PAYROLL_ADMIN xem được.</li>
 *   <li>Không ai tự sửa hồ sơ lương / bảng lương của chính mình.</li>
 * </ul>
 */
@Component
public class PayrollAccess {
    private final UserScopeService scopes;

    public PayrollAccess(UserScopeService scopes) {
        this.scopes = scopes;
    }

    public record Actor(UUID userId, UUID storeId, String storeName, boolean view, boolean manage, boolean approve, boolean pay,
                        boolean export, boolean salaryView, boolean salaryManage, boolean admin) {
        public boolean global() {
            return storeId == null;
        }

        public boolean globalAdmin() {
            return admin && storeId == null;
        }

        public boolean inScope(UUID periodStoreId) {
            return storeId == null || storeId.equals(periodStoreId);
        }

        public PayrollDtos.Permissions permissions() {
            return new PayrollDtos.Permissions(view || admin, manage || admin, approve || admin, pay || admin, export || admin,
                    salaryView || salaryManage || admin, salaryManage || admin, admin, globalAdmin());
        }
    }

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user))
            throw BusinessException.unauthorized("Chưa đăng nhập bằng tài khoản nhân viên");
        return user.userId();
    }

    public Actor current() {
        UUID userId = currentUserId();
        Set<String> codes = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        boolean admin = codes.contains("PAYROLL_ADMIN");
        if (!admin && !codes.contains("PAYROLL_VIEW") && !codes.contains("SALARY_VIEW") && !codes.contains("SALARY_MANAGE"))
            throw BusinessException.forbidden("Bạn không có quyền truy cập dữ liệu lương");
        UserScope scope = scopes.resolve(userId);
        return new Actor(userId, scope.isGlobal() ? null : scope.storeId(), scope.isGlobal() ? null : scope.storeName(),
                codes.contains("PAYROLL_VIEW"), codes.contains("PAYROLL_MANAGE"), codes.contains("PAYROLL_APPROVE"),
                codes.contains("PAYROLL_PAY"), codes.contains("PAYROLL_EXPORT"), codes.contains("SALARY_VIEW"),
                codes.contains("SALARY_MANAGE"), admin);
    }

    static void require(boolean permitted, String message) {
        if (!permitted) throw BusinessException.forbidden(message);
    }

    /** Bộ lọc cửa hàng: HR cửa hàng luôn bị ép về cửa hàng mình. */
    static UUID storeFilter(Actor actor, UUID requested) {
        if (actor.global()) return requested;
        if (requested != null && !requested.equals(actor.storeId()))
            throw BusinessException.forbidden("Bạn không có quyền xem dữ liệu lương cửa hàng này");
        return actor.storeId();
    }

    static void requirePeriodScope(Actor actor, UUID periodStoreId) {
        if (!actor.inScope(periodStoreId)) throw BusinessException.forbidden("Kỳ lương này không thuộc phạm vi của bạn");
    }

    // ------------------------------------------------------------------ điều kiện SQL
    // Tham số: :global, :scopeStore, :admin

    /** Kỳ lương {@code p} trong phạm vi. */
    static String periodGuard(String p) {
        return "(:global or " + p + ".store_id = :scopeStore)";
    }

    /** Dòng lương {@code r}: ẩn hồ sơ bảo mật với người không có PAYROLL_ADMIN. */
    static String confidentialGuard(String r) {
        return "(:admin or not exists (select 1 from salary_structures cs where cs.id = " + r + ".salary_structure_id and cs.confidential))";
    }

    /** Nhân viên {@code u} thuộc phạm vi (HR cửa hàng: nhân viên đang phân công cửa hàng mình). */
    static String employeeGuard(String u) {
        return "(:global or exists (select 1 from store_staffs gs where gs.user_id = " + u + ".id and gs.active and gs.store_id = :scopeStore))";
    }

    static MapSqlParameterSource params(Actor actor) {
        return new MapSqlParameterSource("global", actor.global())
                .addValue("scopeStore", actor.storeId(), java.sql.Types.OTHER)
                .addValue("admin", actor.admin());
    }
}
