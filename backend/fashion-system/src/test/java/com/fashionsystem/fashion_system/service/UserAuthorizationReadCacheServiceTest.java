package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.UserRoleDto;
import com.fashionsystem.fashion_system.dto.UserPermissionDto;
import com.fashionsystem.fashion_system.entity.Permission;
import com.fashionsystem.fashion_system.entity.PermissionEffect;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.entity.UserPermission;
import com.fashionsystem.fashion_system.entity.UserRole;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserAuthorizationReadCacheServiceTest.CacheTestConfiguration.class)
class UserAuthorizationReadCacheServiceTest {

    @Autowired RoleAssignmentAdministrationService service;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRoleRepository userRoleRepository;
    @Autowired PermissionRepository permissionRepository;
    @Autowired UserPermissionRepository userPermissionRepository;
    @Autowired UserRoleMapper userRoleMapper;
    @Autowired UserPermissionMapper userPermissionMapper;
    @Autowired CacheManager cacheManager;

    @Test
    void userRoleListUsesCacheAndRoleRemovalEvictsOnlyThatUserSecurityCaches() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UserRole entity = UserRole.builder().userId(userId).roleId(roleId).build();
        UserRoleDto dto = UserRoleDto.builder().userId(userId).roleId(roleId).build();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRoleRepository.findAllByUserId(userId)).thenReturn(List.of(entity));
        when(userRoleMapper.toDto(entity)).thenReturn(dto);

        assertThat(service.getUserRoles(userId)).containsExactly(dto);
        assertThat(service.getUserRoles(userId)).containsExactly(dto);
        verify(userRoleRepository, times(1)).findAllByUserId(userId);

        cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).put(userId, List.of());
        cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).put(otherUserId, List.of());
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(Role.builder().id(roleId).build()));
        when(userRoleRepository.deleteAssignment(userId, roleId)).thenReturn(1);

        service.removeUserRole(userId, roleId);

        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_USER_ROLE_LIST).get(userId)).isNull();
        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).get(userId)).isNull();
        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).get(otherUserId)).isNotNull();
    }

    @Test
    void userPermissionListUsesCacheAndOverrideRemovalEvictsThatUsersEffectivePermissions() {
        UUID userId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        UserPermission entity = UserPermission.builder()
                .userId(userId).permissionId(permissionId)
                .effect(PermissionEffect.DENY).scope(PermissionScope.ALL).build();
        UserPermissionDto dto = UserPermissionDto.builder()
                .userId(userId).permissionId(permissionId)
                .effect(PermissionEffect.DENY).scope(PermissionScope.ALL).build();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(userPermissionRepository.findAllByUserId(userId)).thenReturn(List.of(entity));
        when(userPermissionMapper.toDto(entity)).thenReturn(dto);

        assertThat(service.getUserPermissions(userId)).containsExactly(dto);
        assertThat(service.getUserPermissions(userId)).containsExactly(dto);
        verify(userPermissionRepository, times(1)).findAllByUserId(userId);

        cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).put(userId, List.of());
        when(permissionRepository.findById(permissionId))
                .thenReturn(Optional.of(Permission.builder().id(permissionId).build()));
        when(userPermissionRepository.deleteOverride(userId, permissionId)).thenReturn(1);

        service.removeUserPermission(userId, permissionId);

        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_USER_PERMISSION_LIST).get(userId)).isNull();
        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).get(userId)).isNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheNames.AUTHORIZATION_USER_ROLE_LIST,
                    CacheNames.AUTHORIZATION_USER_PERMISSION_LIST,
                    CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS,
                    CacheNames.AUTHORIZATION_ROLE_LIST,
                    CacheNames.AUTHORIZATION_ROLE_DETAIL);
        }
        @Bean RoleRepository roleRepository() { return mock(RoleRepository.class); }
        @Bean PermissionRepository permissionRepository() { return mock(PermissionRepository.class); }
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean RolePermissionRepository rolePermissionRepository() { return mock(RolePermissionRepository.class); }
        @Bean UserRoleRepository userRoleRepository() { return mock(UserRoleRepository.class); }
        @Bean UserPermissionRepository userPermissionRepository() { return mock(UserPermissionRepository.class); }
        @Bean RoleMapper roleMapper() { return mock(RoleMapper.class); }
        @Bean UserRoleMapper userRoleMapper() { return mock(UserRoleMapper.class); }
        @Bean UserPermissionMapper userPermissionMapper() { return mock(UserPermissionMapper.class); }
        @Bean AuthorizationAdministrationMapper administrationMapper() { return mock(AuthorizationAdministrationMapper.class); }
        @Bean RoleAssignmentAdministrationService service(
                RoleRepository roles, PermissionRepository permissions, UserRepository users,
                RolePermissionRepository rolePermissions, UserRoleRepository userRoles,
                UserPermissionRepository userPermissions, RoleMapper roleMapper,
                UserRoleMapper userRoleMapper, UserPermissionMapper userPermissionMapper,
                AuthorizationAdministrationMapper administrationMapper) {
            return new RoleAssignmentAdministrationService(
                    roles, permissions, users, rolePermissions, userRoles, userPermissions,
                    roleMapper, userRoleMapper, userPermissionMapper, administrationMapper);
        }
    }
}
