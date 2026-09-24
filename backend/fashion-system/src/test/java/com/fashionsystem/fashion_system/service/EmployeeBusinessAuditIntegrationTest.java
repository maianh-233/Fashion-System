package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fashionsystem.fashion_system.repository.AuthAuditLogRepository;
import org.junit.jupiter.api.Test;

class EmployeeBusinessAuditIntegrationTest {
    @Test
    void businessActionsCannotReachTheAuthRepository() {
        var repository = mock(AuthAuditLogRepository.class);
        var service = new AuthAuditService(repository);
        for (String action : new String[]{"EMPLOYEE_CREATED", "EMPLOYEE_UPDATED", "SUBORDINATE_ASSIGNED", "SUBORDINATE_REMOVED", "UNKNOWN"}) {
            assertThatThrownBy(() -> service.record(null, action, "business mutation"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void employeeServiceHasNoAuthAuditDependency() {
        assertThat(EmployeeAdministrationService.class.getDeclaredFields())
                .noneMatch(field -> field.getType().equals(AuthAuditService.class));
    }

    @Test
    void allThreeAuthActionsStillPersist() {
        var repository = mock(AuthAuditLogRepository.class);
        var service = new AuthAuditService(repository);
        for (String action : new String[]{"LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT"}) service.record(null, action, "auth");
        verify(repository, times(3)).save(any());
    }
}
