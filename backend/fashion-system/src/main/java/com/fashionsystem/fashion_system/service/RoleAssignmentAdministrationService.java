package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.*;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.*;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD role và quản trị các bảng liên kết authorization. */
@Service
@RequiredArgsConstructor
public class RoleAssignmentAdministrationService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserPermissionMapper userPermissionMapper;
    private final AuthorizationAdministrationMapper administrationMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_ROLE_LIST, key = "'all'")
    public List<RoleDto> getRoles() {
        return new ArrayList<>(roleRepository.findAllByOrderByCodeAsc().stream()
                .map(roleMapper::toDto).toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, key = "#roleId")
    public RoleDetails getRole(UUID roleId) {
        Role role = requireRole(roleId);
        return administrationMapper.toRoleDetails(
                roleMapper.toDto(role), rolePermissionRepository.findPermissionDetailsByRoleId(roleId));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_LIST, allEntries = true)
    public RoleDto createRole(RoleDto request) {
        ensureRoleCodeAvailable(request.getCode(), null);
        return roleMapper.toDto(roleRepository.save(roleMapper.toEntity(request)));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, key = "#roleId")
    })
    public RoleDto updateRole(UUID roleId, RoleDto request) {
        Role role = requireRole(roleId);
        ensureRoleCodeAvailable(request.getCode(), roleId);
        roleMapper.updateEntity(request, role);
        return roleMapper.toDto(roleRepository.save(role));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, key = "#roleId")
    })
    public void deleteRole(UUID roleId) {
        requireRole(roleId);
        if (rolePermissionRepository.existsByRoleId(roleId) || userRoleRepository.existsByRoleId(roleId)) {
            throw BusinessException.invalidState("Không thể xóa role đang có permission hoặc đang được gán cho user");
        }
        roleRepository.deleteById(roleId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, key = "#roleId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public RoleDetails assignRolePermission(UUID roleId, RolePermissionGrant request) {
        requireRole(roleId);
        requirePermission(request.permissionId());
        rolePermissionRepository.save(RolePermission.builder()
                .roleId(roleId).permissionId(request.permissionId()).scope(request.scope()).build());
        return getRole(roleId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, key = "#roleId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public RoleDetails replaceRolePermissions(UUID roleId, List<RolePermissionGrant> requests) {
        requireRole(roleId);
        Map<UUID, RolePermissionGrant> grants = uniqueByPermissionId(requests);
        requirePermissions(grants.keySet());
        rolePermissionRepository.deleteAllForRole(roleId);
        rolePermissionRepository.saveAll(grants.values().stream()
                .map(grant -> RolePermission.builder()
                        .roleId(roleId).permissionId(grant.permissionId()).scope(grant.scope()).build())
                .toList());
        return getRole(roleId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, key = "#roleId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public void removeRolePermission(UUID roleId, UUID permissionId) {
        requireRole(roleId);
        requirePermission(permissionId);
        if (rolePermissionRepository.deleteGrant(roleId, permissionId) == 0) {
            throw BusinessException.notFound("Role permission không tồn tại");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_USER_ROLE_LIST, key = "#userId")
    public List<UserRoleDto> getUserRoles(UUID userId) {
        requireUser(userId);
        return new ArrayList<>(userRoleRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(UserRole::getRoleId))
                .map(userRoleMapper::toDto).toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_USER_ROLE_LIST, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, key = "#userId")
    })
    public List<UserRoleDto> assignUserRole(UUID userId, UUID roleId) {
        requireUser(userId);
        requireRole(roleId);
        userRoleRepository.save(UserRole.builder()
                .userId(userId).roleId(roleId).assignedAt(LocalDateTime.now()).build());
        return getUserRoles(userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_USER_ROLE_LIST, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, key = "#userId")
    })
    public List<UserRoleDto> replaceUserRoles(UUID userId, RoleIdsRequest request) {
        requireUser(userId);
        requireRoles(request.roleIds());
        LocalDateTime now = LocalDateTime.now();
        userRoleRepository.deleteAllForUser(userId);
        userRoleRepository.saveAll(request.roleIds().stream()
                .map(roleId -> UserRole.builder().userId(userId).roleId(roleId).assignedAt(now).build())
                .toList());
        return getUserRoles(userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_USER_ROLE_LIST, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, key = "#userId")
    })
    public void removeUserRole(UUID userId, UUID roleId) {
        requireUser(userId);
        requireRole(roleId);
        if (userRoleRepository.deleteAssignment(userId, roleId) == 0) {
            throw BusinessException.notFound("User role không tồn tại");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_USER_PERMISSION_LIST, key = "#userId")
    public List<UserPermissionDto> getUserPermissions(UUID userId) {
        requireUser(userId);
        return new ArrayList<>(userPermissionRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(UserPermission::getPermissionId))
                .map(userPermissionMapper::toDto).toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_USER_PERMISSION_LIST, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, key = "#userId")
    })
    public List<UserPermissionDto> assignUserPermission(UUID userId, UserPermissionGrant request) {
        requireUser(userId);
        requirePermission(request.permissionId());
        userPermissionRepository.save(toUserPermission(userId, request));
        return getUserPermissions(userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_USER_PERMISSION_LIST, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, key = "#userId")
    })
    public List<UserPermissionDto> replaceUserPermissions(UUID userId, List<UserPermissionGrant> requests) {
        requireUser(userId);
        Map<UUID, UserPermissionGrant> grants = uniqueUserOverrides(requests);
        requirePermissions(grants.keySet());
        userPermissionRepository.deleteAllForUser(userId);
        userPermissionRepository.saveAll(grants.values().stream()
                .map(grant -> toUserPermission(userId, grant)).toList());
        return getUserPermissions(userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_USER_PERMISSION_LIST, key = "#userId"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, key = "#userId")
    })
    public void removeUserPermission(UUID userId, UUID permissionId) {
        requireUser(userId);
        requirePermission(permissionId);
        if (userPermissionRepository.deleteOverride(userId, permissionId) == 0) {
            throw BusinessException.notFound("User permission không tồn tại");
        }
    }

    private UserPermission toUserPermission(UUID userId, UserPermissionGrant grant) {
        return UserPermission.builder().userId(userId).permissionId(grant.permissionId())
                .effect(grant.effect()).scope(grant.scope()).build();
    }

    private Map<UUID, RolePermissionGrant> uniqueByPermissionId(List<RolePermissionGrant> requests) {
        Map<UUID, RolePermissionGrant> grants = new LinkedHashMap<>();
        for (RolePermissionGrant request : requests) {
            if (grants.putIfAbsent(request.permissionId(), request) != null) {
                throw BusinessException.badRequest("Danh sách có permission bị trùng");
            }
        }
        return grants;
    }

    private Map<UUID, UserPermissionGrant> uniqueUserOverrides(List<UserPermissionGrant> requests) {
        Map<UUID, UserPermissionGrant> grants = new LinkedHashMap<>();
        for (UserPermissionGrant request : requests) {
            if (grants.putIfAbsent(request.permissionId(), request) != null) {
                throw BusinessException.badRequest("Danh sách có user permission bị trùng");
            }
        }
        return grants;
    }

    private void requirePermissions(java.util.Collection<UUID> permissionIds) {
        if (permissionIds.isEmpty()) return;
        Set<UUID> found = permissionRepository.findAllById(permissionIds).stream()
                .map(Permission::getId).collect(Collectors.toSet());
        permissionIds.stream().filter(id -> !found.contains(id)).findFirst()
                .ifPresent(id -> { throw BusinessException.notFound("Permission không tồn tại: " + id); });
    }

    private void requireRoles(java.util.Collection<UUID> roleIds) {
        if (roleIds.isEmpty()) return;
        Set<UUID> found = roleRepository.findAllById(roleIds).stream()
                .map(Role::getId).collect(Collectors.toSet());
        roleIds.stream().filter(id -> !found.contains(id)).findFirst()
                .ifPresent(id -> { throw BusinessException.notFound("Role không tồn tại: " + id); });
    }

    private Role requireRole(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Role không tồn tại"));
    }

    private Permission requirePermission(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Permission không tồn tại"));
    }

    private void requireUser(UUID id) {
        if (!userRepository.existsById(id)) throw BusinessException.notFound("User không tồn tại");
    }

    private void ensureRoleCodeAvailable(String code, UUID excludedId) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null ? roleRepository.existsByCode(normalized)
                : roleRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Role code đã tồn tại");
    }
}
