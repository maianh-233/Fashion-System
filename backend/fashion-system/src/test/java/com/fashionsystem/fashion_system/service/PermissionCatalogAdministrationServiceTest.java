package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.entity.Module;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionCatalogAdministrationServiceTest {
    @Test
    void doesNotDeleteModuleThatStillContainsGroups() {
        ModuleRepository moduleRepository = mock(ModuleRepository.class);
        PermissionGroupRepository groupRepository = mock(PermissionGroupRepository.class);
        UUID moduleId = UUID.randomUUID();
        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(Module.builder().id(moduleId).build()));
        when(groupRepository.existsByModuleId(moduleId)).thenReturn(true);
        PermissionCatalogAdministrationService service = new PermissionCatalogAdministrationService(
                moduleRepository,
                groupRepository,
                mock(PermissionRepository.class),
                mock(RolePermissionRepository.class),
                mock(UserPermissionRepository.class),
                mock(ModuleMapper.class),
                mock(PermissionGroupMapper.class),
                mock(PermissionMapper.class),
                mock(AuthorizationAdministrationMapper.class));

        assertThatThrownBy(() -> service.deleteModule(moduleId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang chứa permission group");
        verify(moduleRepository, never()).deleteById(moduleId);
    }
}
