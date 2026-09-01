package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.RoleDetails;
import com.fashionsystem.fashion_system.dto.RoleDto;
import com.fashionsystem.fashion_system.entity.Permission;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RoleReadCacheServiceTest.CacheTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoleReadCacheServiceTest {

    @Autowired private RoleAssignmentAdministrationService service;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private RoleMapper roleMapper;
    @Autowired private AuthorizationAdministrationMapper administrationMapper;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        reset(roleRepository, permissionRepository, rolePermissionRepository, roleMapper, administrationMapper);
        cacheManager.getCache(CacheNames.AUTHORIZATION_ROLE_DETAIL).clear();
        cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).clear();
    }

    @Test
    void roleDetailUsesCacheAfterFirstDatabaseRead() {
        UUID roleId = UUID.randomUUID();
        Role role = Role.builder().id(roleId).code("ADMIN").build();
        RoleDto roleDto = RoleDto.builder().id(roleId).code("ADMIN").name("Admin").build();
        RoleDetails response = new RoleDetails(roleDto, List.of());
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(roleMapper.toDto(role)).thenReturn(roleDto);
        when(rolePermissionRepository.findPermissionDetailsByRoleId(roleId)).thenReturn(List.of());
        when(administrationMapper.toRoleDetails(roleDto, List.of())).thenReturn(response);

        assertThat(service.getRole(roleId)).isEqualTo(response);
        assertThat(service.getRole(roleId)).isEqualTo(response);

        verify(roleRepository, times(1)).findById(roleId);
        verify(rolePermissionRepository, times(1)).findPermissionDetailsByRoleId(roleId);
    }

    @Test
    void removingRolePermissionEvictsRoleDetail() {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        UUID affectedUserId = UUID.randomUUID();
        RoleDetails cached = new RoleDetails(
                RoleDto.builder().id(roleId).code("ADMIN").name("Admin").build(), List.of());
        cacheManager.getCache(CacheNames.AUTHORIZATION_ROLE_DETAIL).put(roleId, cached);
        cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS)
                .put(affectedUserId, List.of());
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(Role.builder().id(roleId).build()));
        when(permissionRepository.findById(permissionId))
                .thenReturn(Optional.of(Permission.builder().id(permissionId).build()));
        when(rolePermissionRepository.deleteGrant(roleId, permissionId)).thenReturn(1);

        service.removeRolePermission(roleId, permissionId);

        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_ROLE_DETAIL).get(roleId)).isNull();
        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS).get(affectedUserId)).isNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheNames.AUTHORIZATION_ROLE_LIST,
                    CacheNames.AUTHORIZATION_ROLE_DETAIL,
                    CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS);
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
        @Bean AuthorizationAdministrationMapper administrationMapper() {
            return mock(AuthorizationAdministrationMapper.class);
        }

        @Bean
        RoleAssignmentAdministrationService service(
                RoleRepository roleRepository,
                PermissionRepository permissionRepository,
                UserRepository userRepository,
                RolePermissionRepository rolePermissionRepository,
                UserRoleRepository userRoleRepository,
                UserPermissionRepository userPermissionRepository,
                RoleMapper roleMapper,
                UserRoleMapper userRoleMapper,
                UserPermissionMapper userPermissionMapper,
                AuthorizationAdministrationMapper administrationMapper) {
            return new RoleAssignmentAdministrationService(
                    roleRepository, permissionRepository, userRepository,
                    rolePermissionRepository, userRoleRepository, userPermissionRepository,
                    roleMapper, userRoleMapper, userPermissionMapper, administrationMapper);
        }
    }
}
