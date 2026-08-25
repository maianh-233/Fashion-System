package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.*;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.*;
import com.fashionsystem.fashion_system.service.RoleAssignmentAdministrationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** CRUD role và API gán/replace/xóa các liên kết authorization. */
@RestController
@RequestMapping("/api/admin/authorization")
@PreAuthorize("hasAuthority(@authorizationManagementProperties.permissionCode)")
@RequiredArgsConstructor
public class RoleAssignmentAdministrationController {
    private final RoleAssignmentAdministrationService service;

    @GetMapping("/roles")
    public List<RoleDto> getRoles() { return service.getRoles(); }

    @GetMapping("/roles/{roleId}")
    public RoleDetails getRole(@PathVariable UUID roleId) { return service.getRole(roleId); }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleDto createRole(@Valid @RequestBody RoleDto request) { return service.createRole(request); }

    @PutMapping("/roles/{roleId}")
    public RoleDto updateRole(@PathVariable UUID roleId, @Valid @RequestBody RoleDto request) {
        return service.updateRole(roleId, request);
    }

    @DeleteMapping("/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable UUID roleId) { service.deleteRole(roleId); }

    @PostMapping("/roles/{roleId}/permissions")
    public RoleDetails assignRolePermission(
            @PathVariable UUID roleId, @Valid @RequestBody RolePermissionGrant request) {
        return service.assignRolePermission(roleId, request);
    }

    @PutMapping("/roles/{roleId}/permissions")
    public RoleDetails replaceRolePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody List<@Valid RolePermissionGrant> requests) {
        return service.replaceRolePermissions(roleId, requests);
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRolePermission(@PathVariable UUID roleId, @PathVariable UUID permissionId) {
        service.removeRolePermission(roleId, permissionId);
    }

    @GetMapping("/users/{userId}/roles")
    public List<UserRoleDto> getUserRoles(@PathVariable UUID userId) { return service.getUserRoles(userId); }

    @PostMapping("/users/{userId}/roles/{roleId}")
    public List<UserRoleDto> assignUserRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        return service.assignUserRole(userId, roleId);
    }

    @PutMapping("/users/{userId}/roles")
    public List<UserRoleDto> replaceUserRoles(
            @PathVariable UUID userId, @Valid @RequestBody RoleIdsRequest request) {
        return service.replaceUserRoles(userId, request);
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUserRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        service.removeUserRole(userId, roleId);
    }

    @GetMapping("/users/{userId}/permissions")
    public List<UserPermissionDto> getUserPermissions(@PathVariable UUID userId) {
        return service.getUserPermissions(userId);
    }

    @PostMapping("/users/{userId}/permissions")
    public List<UserPermissionDto> assignUserPermission(
            @PathVariable UUID userId, @Valid @RequestBody UserPermissionGrant request) {
        return service.assignUserPermission(userId, request);
    }

    @PutMapping("/users/{userId}/permissions")
    public List<UserPermissionDto> replaceUserPermissions(
            @PathVariable UUID userId,
            @Valid @RequestBody List<@Valid UserPermissionGrant> requests) {
        return service.replaceUserPermissions(userId, requests);
    }

    @DeleteMapping("/users/{userId}/permissions/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUserPermission(@PathVariable UUID userId, @PathVariable UUID permissionId) {
        service.removeUserPermission(userId, permissionId);
    }
}
