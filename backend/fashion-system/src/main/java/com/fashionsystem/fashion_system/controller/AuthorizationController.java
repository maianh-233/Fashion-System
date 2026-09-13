
package com.fashionsystem.fashion_system.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fashionsystem.fashion_system.dto.EffectivePermissionDto;
import com.fashionsystem.fashion_system.dto.PermissionCheckResponse;
import com.fashionsystem.fashion_system.dto.MyPermissionsResponse;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.AuthorizationService;

import lombok.RequiredArgsConstructor;

/** API self-service để frontend dựng menu/button từ quyền hiệu lực. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class AuthorizationController {
    private final AuthorizationService authorizationService;

    @GetMapping("/authorization/me/permissions")
    public List<EffectivePermissionDto> getMyEffectivePermissions(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return authorizationService.getEffectivePermissions(user.userId());
    }

    @GetMapping("/me/permissions")
    public MyPermissionsResponse getCurrentUserPermissionTree(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return authorizationService.getCurrentUserPermissions(user);
    }

    @GetMapping("/authorization/me/check")
    public PermissionCheckResponse checkMyPermission(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String permissionCode,
            @RequestParam(defaultValue = "SELF") PermissionScope scope) {
        return new PermissionCheckResponse(permissionCode, scope,
                authorizationService.hasPermission(user.userId(), permissionCode, scope));
    }
}
