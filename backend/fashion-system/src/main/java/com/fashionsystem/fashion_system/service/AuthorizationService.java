package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.EffectivePermissionDto;
import com.fashionsystem.fashion_system.dto.MyPermissionsResponse;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.EffectivePermissionMapper;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Điểm quyết định authorization duy nhất của backend. */
@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final EffectivePermissionQueryService effectivePermissionQueryService;
    private final EffectivePermissionMapper permissionMapper;

    /** Kiểm tra permission mà không yêu cầu phạm vi dữ liệu cụ thể. */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, String permissionCode) {
        return findPermission(userId, permissionCode).isPresent();
    }

    /** Kiểm tra permission và bảo đảm scope hiệu lực bao phủ scope yêu cầu. */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, String permissionCode, PermissionScope requiredScope) {
        PermissionScope required = requiredScope == null ? PermissionScope.SELF : requiredScope;
        return findPermission(userId, permissionCode)
                .map(EffectivePermissionDto::scope)
                .filter(scope -> scope.covers(required))
                .isPresent();
    }

    /** Overload dành cho SpEL trong @PreAuthorize. */
    public boolean hasPermission(Authentication authentication, String permissionCode, PermissionScope requiredScope) {
        UUID userId = authenticatedUserId(authentication);
        return userId != null && hasPermission(userId, permissionCode, requiredScope);
    }

    /** Overload dành cho SpEL khi chỉ cần kiểm tra action. */
    public boolean hasPermission(Authentication authentication, String permissionCode) {
        UUID userId = authenticatedUserId(authentication);
        return userId != null && hasPermission(userId, permissionCode);
    }

    /**
     * Hợp nhất grants theo thứ tự: user DENY > user ALLOW > role permission.
     * Nhiều role cùng cấp một permission sẽ lấy scope rộng nhất.
     */
    @Transactional(readOnly = true)
    public List<EffectivePermissionDto> getEffectivePermissions(UUID userId) {
        return effectivePermissionQueryService.getEffectivePermissions(userId);
    }

    /** Trả cây quyền của principal hiện tại; không nhận userId từ request. */
    @Transactional(readOnly = true)
    public MyPermissionsResponse getCurrentUserPermissions(AuthenticatedUser currentUser) {
        if (currentUser == null) {
            throw BusinessException.forbidden("Endpoint chỉ dành cho tài khoản nội bộ");
        }
        return permissionMapper.toTree(getEffectivePermissions(currentUser.userId()));
    }

    private Optional<EffectivePermissionDto> findPermission(UUID userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) return Optional.empty();
        String normalized = normalizeCode(permissionCode);
        return getEffectivePermissions(userId).stream()
                .filter(permission -> permission.permissionCode().equals(normalized))
                .findFirst();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private UUID authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return authentication.getPrincipal() instanceof AuthenticatedUser user ? user.userId() : null;
    }
}
