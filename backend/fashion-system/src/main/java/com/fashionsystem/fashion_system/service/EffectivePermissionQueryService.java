package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.EffectivePermissionDto;
import com.fashionsystem.fashion_system.entity.PermissionEffect;
import com.fashionsystem.fashion_system.mapper.EffectivePermissionMapper;
import com.fashionsystem.fashion_system.repository.EffectivePermissionRow;
import com.fashionsystem.fashion_system.repository.RolePermissionRepository;
import com.fashionsystem.fashion_system.repository.UserPermissionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Computes and caches the security-sensitive effective permission set for one user. */
@Service
@RequiredArgsConstructor
public class EffectivePermissionQueryService {

    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final EffectivePermissionMapper permissionMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS, key = "#userId")
    public List<EffectivePermissionDto> getEffectivePermissions(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Map<String, EffectivePermissionDto> effective = new HashMap<>();

        for (EffectivePermissionRow row : rolePermissionRepository.findRoleGrantsByUserId(userId)) {
            EffectivePermissionDto candidate = permissionMapper.toDto(row, "ROLE");
            effective.merge(candidate.permissionCode(), candidate, (current, replacement) ->
                    replacement.scope().covers(current.scope()) ? replacement : current);
        }

        for (EffectivePermissionRow row : userPermissionRepository.findOverridesByUserId(userId)) {
            String code = normalizeCode(row.getPermissionCode());
            PermissionEffect effect = PermissionEffect.valueOf(row.getEffect());
            if (effect == PermissionEffect.DENY) {
                effective.remove(code);
            } else {
                effective.put(code, permissionMapper.toDto(row, "USER"));
            }
        }

        return new ArrayList<>(effective.values().stream()
                .sorted(Comparator.comparing(EffectivePermissionDto::moduleSortOrder)
                        .thenComparing(EffectivePermissionDto::moduleCode)
                        .thenComparing(EffectivePermissionDto::groupCode)
                        .thenComparing(EffectivePermissionDto::permissionCode))
                .toList());
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
