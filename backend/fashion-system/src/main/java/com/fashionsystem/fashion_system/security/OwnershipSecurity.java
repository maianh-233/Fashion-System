package com.fashionsystem.fashion_system.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Cung cấp kiểm tra ownership phía server cho biểu thức @PreAuthorize. */
@Component("ownershipSecurity")
public class OwnershipSecurity {

    /**
     * So sánh ownerId của bản ghi với userId trong principal tin cậy.
     *
     * @param ownerId mã chủ sở hữu lấy từ dữ liệu server
     * @return true khi user hiện tại sở hữu bản ghi
     */
    public boolean isOwner(UUID ownerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser principal
                && principal.userId().equals(ownerId);
    }

    /**
     * Cho phép chủ sở hữu hoặc user có permission quản trị tương ứng.
     *
     * @param ownerId mã chủ sở hữu lấy từ bản ghi phía server
     * @param authority permission code cần có nếu không phải chủ sở hữu
     * @return true khi ownership hoặc permission hợp lệ
     */
    public boolean isOwnerOrHasAuthority(UUID ownerId, String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && isOwner(ownerId))
                || (authentication != null && authentication.getAuthorities().stream()
                        .anyMatch(granted -> granted.getAuthority().equals(authority)));
    }

    /**
     * Kiểm tra principal hiện tại có phải đúng khách hàng được yêu cầu hay không.
     */
    public boolean isCustomer(UUID customerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedCustomer principal
                && principal.customerId().equals(customerId);
    }

    /**
     * Cho phép khách hàng sở hữu dữ liệu hoặc một nhân viên đã xác thực truy cập.
     */
    public boolean isCustomerOrEmployee(UUID customerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isCustomer(customerId)
                || (authentication != null
                        && authentication.isAuthenticated()
                        && authentication.getPrincipal() instanceof AuthenticatedUser);
    }
}
