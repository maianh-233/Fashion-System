package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.mapper.EffectivePermissionMapper;
import com.fashionsystem.fashion_system.repository.EffectivePermissionRow;
import com.fashionsystem.fashion_system.repository.RolePermissionRepository;
import com.fashionsystem.fashion_system.repository.UserPermissionRepository;
import java.util.List;
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
@ContextConfiguration(classes = EffectivePermissionReadCacheServiceTest.CacheTestConfiguration.class)
class EffectivePermissionReadCacheServiceTest {

    @Autowired EffectivePermissionQueryService service;
    @Autowired RolePermissionRepository rolePermissionRepository;
    @Autowired UserPermissionRepository userPermissionRepository;

    @Test
    void repeatedPermissionChecksUseOneComputedPermissionSet() {
        UUID userId = UUID.randomUUID();
        when(rolePermissionRepository.findRoleGrantsByUserId(userId))
                .thenReturn(List.of(row("EMPLOYEE_VIEW", "TEAM", null)));
        when(userPermissionRepository.findOverridesByUserId(userId)).thenReturn(List.of());

        assertThat(service.getEffectivePermissions(userId).getFirst().scope())
                .isEqualTo(PermissionScope.TEAM);
        assertThat(service.getEffectivePermissions(userId).getFirst().scope())
                .isEqualTo(PermissionScope.TEAM);

        verify(rolePermissionRepository, times(1)).findRoleGrantsByUserId(userId);
        verify(userPermissionRepository, times(1)).findOverridesByUserId(userId);
    }

    private EffectivePermissionRow row(String code, String scope, String effect) {
        UUID permissionId = UUID.randomUUID();
        return new EffectivePermissionRow() {
            public UUID getPermissionId() { return permissionId; }
            public String getPermissionCode() { return code; }
            public String getPermissionName() { return code; }
            public String getGroupCode() { return "EMPLOYEE"; }
            public String getGroupName() { return "Employee"; }
            public String getModuleCode() { return "HUMAN_RESOURCE"; }
            public String getModuleName() { return "Human resource"; }
            public String getModuleDescription() { return null; }
            public String getModuleIcon() { return "users"; }
            public Integer getModuleSortOrder() { return 1; }
            public String getScope() { return scope; }
            public String getEffect() { return effect; }
        };
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS);
        }
        @Bean RolePermissionRepository rolePermissionRepository() { return mock(RolePermissionRepository.class); }
        @Bean UserPermissionRepository userPermissionRepository() { return mock(UserPermissionRepository.class); }
        @Bean EffectivePermissionMapper effectivePermissionMapper() { return new EffectivePermissionMapper(); }
        @Bean EffectivePermissionQueryService service(
                RolePermissionRepository roles,
                UserPermissionRepository users,
                EffectivePermissionMapper mapper) {
            return new EffectivePermissionQueryService(roles, users, mapper);
        }
    }
}
