package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.EffectivePermissionDto;
import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.mapper.EffectivePermissionMapper;
import com.fashionsystem.fashion_system.repository.EffectivePermissionRow;
import com.fashionsystem.fashion_system.repository.RolePermissionRepository;
import com.fashionsystem.fashion_system.repository.UserPermissionRepository;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock UserPermissionRepository userPermissionRepository;

    @Test
    void choosesWidestScopeFromRoles() {
        UUID userId = UUID.randomUUID();
        when(rolePermissionRepository.findRoleGrantsByUserId(userId)).thenReturn(List.of(
                row("EMPLOYEE_VIEW", "SELF", null), row("EMPLOYEE_VIEW", "TEAM", null)));
        when(userPermissionRepository.findOverridesByUserId(userId)).thenReturn(List.of());

        AuthorizationService service = service();

        assertThat(service.getEffectivePermissions(userId))
                .extracting(EffectivePermissionDto::scope)
                .containsExactly(PermissionScope.TEAM);
        assertThat(service.hasPermission(userId, "employee_view", PermissionScope.TEAM)).isTrue();
        assertThat(service.hasPermission(userId, "EMPLOYEE_VIEW", PermissionScope.DEPARTMENT)).isFalse();
    }

    @Test
    void userAllowReplacesRoleScope() {
        UUID userId = UUID.randomUUID();
        when(rolePermissionRepository.findRoleGrantsByUserId(userId))
                .thenReturn(List.of(row("EMPLOYEE_VIEW", "ALL", null)));
        when(userPermissionRepository.findOverridesByUserId(userId))
                .thenReturn(List.of(row("EMPLOYEE_VIEW", "SELF", "ALLOW")));

        AuthorizationService service = service();

        assertThat(service.getEffectivePermissions(userId).getFirst().scope()).isEqualTo(PermissionScope.SELF);
    }

    @Test
    void userDenyRemovesRolePermission() {
        UUID userId = UUID.randomUUID();
        when(rolePermissionRepository.findRoleGrantsByUserId(userId))
                .thenReturn(List.of(row("PAYROLL_APPROVE", "ALL", null)));
        when(userPermissionRepository.findOverridesByUserId(userId))
                .thenReturn(List.of(row("PAYROLL_APPROVE", "ALL", "DENY")));

        AuthorizationService service = service();

        assertThat(service.getEffectivePermissions(userId)).isEmpty();
        assertThat(service.hasPermission(userId, "PAYROLL_APPROVE")).isFalse();
    }

    @Test
    void buildsCurrentUserPermissionTree() {
        UUID userId = UUID.randomUUID();
        when(rolePermissionRepository.findRoleGrantsByUserId(userId)).thenReturn(List.of(
                row("EMPLOYEE_VIEW", "TEAM", null), row("EMPLOYEE_UPDATE", "SELF", null)));
        when(userPermissionRepository.findOverridesByUserId(userId)).thenReturn(List.of());

        var response = service().getCurrentUserPermissions(new AuthenticatedUser(userId, "alice"));

        assertThat(response.modules()).hasSize(1);
        assertThat(response.modules().getFirst().code()).isEqualTo("HUMAN_RESOURCE");
        assertThat(response.modules().getFirst().groups().getFirst().permissions())
                .extracting(permission -> permission.code())
                .containsExactly("EMPLOYEE_UPDATE", "EMPLOYEE_VIEW");
    }

    private AuthorizationService service() {
        EffectivePermissionMapper mapper = new EffectivePermissionMapper();
        return new AuthorizationService(
                new EffectivePermissionQueryService(
                        rolePermissionRepository, userPermissionRepository, mapper),
                mapper);
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
}
