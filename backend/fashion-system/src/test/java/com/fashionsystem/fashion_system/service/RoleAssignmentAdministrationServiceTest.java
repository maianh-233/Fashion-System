package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fashionsystem.fashion_system.dto.AuthorizationAdministrationDto.RolePermissionGrant;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.entity.Role;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.repository.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoleAssignmentAdministrationServiceTest {
    @Test
    void rejectsDuplicatePermissionBeforeReplacingExistingGrants() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(Role.builder().id(roleId).build()));
        RoleAssignmentAdministrationService service = new RoleAssignmentAdministrationService(
                roleRepository,
                mock(PermissionRepository.class),
                mock(UserRepository.class),
                rolePermissionRepository,
                mock(UserRoleRepository.class),
                mock(UserPermissionRepository.class),
                mock(RoleMapper.class),
                mock(UserRoleMapper.class),
                mock(UserPermissionMapper.class),
                mock(AuthorizationAdministrationMapper.class));

        var duplicate = new RolePermissionGrant(permissionId, PermissionScope.TEAM);
        assertThatThrownBy(() -> service.replaceRolePermissions(roleId, List.of(duplicate, duplicate)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permission bị trùng");
        verify(rolePermissionRepository, never()).deleteAllForRole(roleId);
    }
}
