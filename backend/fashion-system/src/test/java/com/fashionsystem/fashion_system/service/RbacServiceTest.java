package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.EffectivePermissionDto;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class RbacServiceTest {
    @Test
    void loadsRoleAndPermissionAuthoritiesForAdmin() {
        UUID userId = UUID.randomUUID();
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.findCodesByUserId(userId)).thenReturn(List.of("ADMIN"));
        when(authorizationService.getEffectivePermissions(userId)).thenReturn(List.of(
                new EffectivePermissionDto(
                        UUID.randomUUID(), "SETTINGS_MANAGE", "Quản lý setting",
                        "SETTINGS", "Cấu hình", "SYSTEM", "Hệ thống",
                        null, "settings", 70, PermissionScope.ALL, "ROLE")));

        RbacService service = new RbacService(authorizationService, roleRepository);

        assertThat(service.loadAuthorities(userId))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "SETTINGS_MANAGE");
    }
}
