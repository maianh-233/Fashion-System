package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.*;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.Catalog;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD catalog Module -> PermissionGroup -> Permission. */
@Service
@RequiredArgsConstructor
public class PermissionCatalogAdministrationService {
    private final ModuleRepository moduleRepository;
    private final PermissionGroupRepository groupRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final ModuleMapper moduleMapper;
    private final PermissionGroupMapper groupMapper;
    private final PermissionMapper permissionMapper;
    private final AuthorizationAdministrationMapper administrationMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_MODULE_LIST, key = "'all'")
    public List<ModuleDto> getModules() {
        return new ArrayList<>(moduleRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(moduleMapper::toDto).toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_MODULE_DETAIL, key = "#id")
    public ModuleDto getModule(UUID id) {
        return moduleMapper.toDto(requireModule(id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ACTIVE_MODULES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_MODULE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public ModuleDto createModule(ModuleDto request) {
        ensureModuleCodeAvailable(request.getCode(), null);
        return moduleMapper.toDto(moduleRepository.save(moduleMapper.toEntity(request)));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ACTIVE_MODULES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_MODULE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_MODULE_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public ModuleDto updateModule(UUID id, ModuleDto request) {
        com.fashionsystem.fashion_system.entity.Module entity = requireModule(id);
        ensureModuleCodeAvailable(request.getCode(), id);
        moduleMapper.updateEntity(request, entity);
        return moduleMapper.toDto(moduleRepository.save(entity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ACTIVE_MODULES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_MODULE_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_MODULE_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public void deleteModule(UUID id) {
        requireModule(id);
        if (groupRepository.existsByModuleId(id)) {
            throw BusinessException.invalidState("Không thể xóa module đang chứa permission group");
        }
        moduleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_GROUP_LIST, key = "'all'")
    public List<PermissionGroupDto> getGroups() {
        return new ArrayList<>(groupRepository.findAllByOrderByCodeAsc().stream()
                .map(groupMapper::toDto).toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_GROUP_DETAIL, key = "#id")
    public PermissionGroupDto getGroup(UUID id) {
        return groupMapper.toDto(requireGroup(id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_GROUP_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public PermissionGroupDto createGroup(PermissionGroupDto request) {
        requireModule(request.getModuleId());
        ensureGroupCodeAvailable(request.getCode(), null);
        return groupMapper.toDto(groupRepository.save(groupMapper.toEntity(request)));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_GROUP_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_GROUP_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public PermissionGroupDto updateGroup(UUID id, PermissionGroupDto request) {
        PermissionGroup entity = requireGroup(id);
        requireModule(request.getModuleId());
        ensureGroupCodeAvailable(request.getCode(), id);
        groupMapper.updateEntity(request, entity);
        return groupMapper.toDto(groupRepository.save(entity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_GROUP_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_GROUP_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public void deleteGroup(UUID id) {
        requireGroup(id);
        if (permissionRepository.existsByGroupId(id)) {
            throw BusinessException.invalidState("Không thể xóa permission group đang chứa permission");
        }
        groupRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_PERMISSION_LIST, key = "'all'")
    public List<PermissionDto> getPermissions() {
        return new ArrayList<>(permissionRepository.findAllByOrderByCodeAsc().stream()
                .map(permissionMapper::toDto).toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_PERMISSION_DETAIL, key = "#id")
    public PermissionDto getPermission(UUID id) {
        return permissionMapper.toDto(requirePermission(id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_PERMISSION_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public PermissionDto createPermission(PermissionDto request) {
        requireGroup(request.getGroupId());
        ensurePermissionCodeAvailable(request.getCode(), null);
        return permissionMapper.toDto(permissionRepository.save(permissionMapper.toEntity(request)));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_PERMISSION_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_PERMISSION_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public PermissionDto updatePermission(UUID id, PermissionDto request) {
        Permission entity = requirePermission(id);
        requireGroup(request.getGroupId());
        ensurePermissionCodeAvailable(request.getCode(), id);
        permissionMapper.updateEntity(request, entity);
        return permissionMapper.toDto(permissionRepository.save(entity));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_PERMISSION_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_PERMISSION_DETAIL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_CATALOG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_ROLE_DETAIL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, allEntries = true)
    })
    public void deletePermission(UUID id) {
        requirePermission(id);
        if (rolePermissionRepository.existsByPermissionId(id)
                || userPermissionRepository.existsByPermissionId(id)) {
            throw BusinessException.invalidState("Không thể xóa permission đang được gán cho role hoặc user");
        }
        permissionRepository.deleteById(id);
    }

    /** Ba query cố định, kể cả khi catalog có nhiều module/group/permission. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_CATALOG, key = "'all'")
    public Catalog getCatalogTree() {
        return administrationMapper.toCatalog(
                moduleRepository.findAllByOrderBySortOrderAscCodeAsc(),
                groupRepository.findAllByOrderByCodeAsc(),
                permissionRepository.findAllByOrderByCodeAsc());
    }

    private com.fashionsystem.fashion_system.entity.Module requireModule(UUID id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Module không tồn tại"));
    }

    private PermissionGroup requireGroup(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Permission group không tồn tại"));
    }

    private Permission requirePermission(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Permission không tồn tại"));
    }

    private void ensureModuleCodeAvailable(String code, UUID excludedId) {
        String normalized = normalizeCode(code);
        boolean exists = excludedId == null ? moduleRepository.existsByCode(normalized)
                : moduleRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Module code đã tồn tại");
    }

    private void ensureGroupCodeAvailable(String code, UUID excludedId) {
        String normalized = normalizeCode(code);
        boolean exists = excludedId == null ? groupRepository.existsByCode(normalized)
                : groupRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Permission group code đã tồn tại");
    }

    private void ensurePermissionCodeAvailable(String code, UUID excludedId) {
        String normalized = normalizeCode(code);
        boolean exists = excludedId == null ? permissionRepository.existsByCode(normalized)
                : permissionRepository.existsByCodeAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("Permission code đã tồn tại");
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
