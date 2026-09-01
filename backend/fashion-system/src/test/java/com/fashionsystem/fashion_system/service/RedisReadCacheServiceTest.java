package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto;
import com.fashionsystem.fashion_system.dto.ModuleDto;
import com.fashionsystem.fashion_system.entity.Module;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.util.List;
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
@ContextConfiguration(classes = RedisReadCacheServiceTest.CacheTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RedisReadCacheServiceTest {

    @Autowired private ModuleService moduleService;
    @Autowired private PermissionCatalogAdministrationService catalogService;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private ModuleMapper moduleMapper;
    @Autowired private CacheManager cacheManager;

    @BeforeEach
    void clearMocksAndCaches() {
        reset(moduleRepository, moduleMapper);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void activeModuleListUsesCacheAfterFirstDatabaseRead() {
        Module entity = Module.builder().code("CATALOG").name("Catalog").active(true).build();
        ModuleDto response = ModuleDto.builder().code("CATALOG").name("Catalog").active(true).build();
        when(moduleRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc()).thenReturn(List.of(entity));
        when(moduleMapper.toDto(entity)).thenReturn(response);

        assertThat(moduleService.getActiveModules()).containsExactly(response);
        assertThat(moduleService.getActiveModules()).containsExactly(response);

        verify(moduleRepository, times(1)).findAllByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Test
    void moduleCreateClearsActiveAdminAndCatalogCaches() {
        ModuleDto request = ModuleDto.builder().code("ORDER").name("Order").active(true).build();
        Module entity = Module.builder().code("ORDER").name("Order").active(true).build();
        when(moduleRepository.existsByCode("ORDER")).thenReturn(false);
        when(moduleMapper.toEntity(request)).thenReturn(entity);
        when(moduleRepository.save(entity)).thenReturn(entity);
        when(moduleMapper.toDto(entity)).thenReturn(request);
        cacheManager.getCache(CacheNames.ACTIVE_MODULES).put("all", List.of(request));
        cacheManager.getCache(CacheNames.AUTHORIZATION_MODULE_LIST).put("all", List.of(request));
        cacheManager.getCache(CacheNames.AUTHORIZATION_CATALOG).put(
                "all", mock(AuthorizationAdministrationDto.Catalog.class));

        assertThat(catalogService.createModule(request)).isEqualTo(request);

        assertThat(cacheManager.getCache(CacheNames.ACTIVE_MODULES).get("all")).isNull();
        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_MODULE_LIST).get("all")).isNull();
        assertThat(cacheManager.getCache(CacheNames.AUTHORIZATION_CATALOG).get("all")).isNull();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheTestConfiguration {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheNames.ACTIVE_MODULES,
                    CacheNames.AUTHORIZATION_MODULE_LIST,
                    CacheNames.AUTHORIZATION_MODULE_DETAIL,
                    CacheNames.AUTHORIZATION_GROUP_LIST,
                    CacheNames.AUTHORIZATION_GROUP_DETAIL,
                    CacheNames.AUTHORIZATION_PERMISSION_LIST,
                    CacheNames.AUTHORIZATION_PERMISSION_DETAIL,
                    CacheNames.AUTHORIZATION_CATALOG,
                    CacheNames.AUTHORIZATION_ROLE_LIST,
                    CacheNames.AUTHORIZATION_ROLE_DETAIL,
                    CacheNames.AUTHORIZATION_EFFECTIVE_PERMISSIONS);
        }

        @Bean ModuleRepository moduleRepository() { return mock(ModuleRepository.class); }
        @Bean PermissionGroupRepository groupRepository() { return mock(PermissionGroupRepository.class); }
        @Bean PermissionRepository permissionRepository() { return mock(PermissionRepository.class); }
        @Bean RolePermissionRepository rolePermissionRepository() { return mock(RolePermissionRepository.class); }
        @Bean UserPermissionRepository userPermissionRepository() { return mock(UserPermissionRepository.class); }
        @Bean ModuleMapper moduleMapper() { return mock(ModuleMapper.class); }
        @Bean PermissionGroupMapper groupMapper() { return mock(PermissionGroupMapper.class); }
        @Bean PermissionMapper permissionMapper() { return mock(PermissionMapper.class); }
        @Bean AuthorizationAdministrationMapper administrationMapper() {
            return mock(AuthorizationAdministrationMapper.class);
        }

        @Bean
        ModuleService moduleService(ModuleRepository repository, ModuleMapper mapper) {
            return new ModuleService(repository, mapper);
        }

        @Bean
        PermissionCatalogAdministrationService catalogService(
                ModuleRepository moduleRepository,
                PermissionGroupRepository groupRepository,
                PermissionRepository permissionRepository,
                RolePermissionRepository rolePermissionRepository,
                UserPermissionRepository userPermissionRepository,
                ModuleMapper moduleMapper,
                PermissionGroupMapper groupMapper,
                PermissionMapper permissionMapper,
                AuthorizationAdministrationMapper administrationMapper) {
            return new PermissionCatalogAdministrationService(
                    moduleRepository, groupRepository, permissionRepository,
                    rolePermissionRepository, userPermissionRepository,
                    moduleMapper, groupMapper, permissionMapper, administrationMapper);
        }
    }
}
