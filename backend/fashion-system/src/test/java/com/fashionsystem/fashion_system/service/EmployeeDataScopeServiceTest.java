package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.PermissionScope;
import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.RoleRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import com.fashionsystem.fashion_system.repository.StoreStaffRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EmployeeDataScopeServiceTest {
    private static final String PERMISSION = "USER_VIEW";

    @Mock AuthorizationService authorizationService;
    @Mock RoleRepository roleRepository;
    @Mock StoreStaffRepository storeStaffRepository;
    @Mock StoreRepository storeRepository;

    private EmployeeDataScopeService service;
    private UUID actorId;
    private UUID storeId;
    private UUID otherStoreId;

    @BeforeEach
    void setUp() {
        service = new EmployeeDataScopeService(
                authorizationService, roleRepository, storeStaffRepository, storeRepository);
        actorId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        otherStoreId = UUID.randomUUID();
    }

    @Test
    void adminResolvesGlobalWithoutLoadingAssignments() {
        allow();
        when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("ADMIN"));

        EmployeeDataScope result = service.resolve(actorId, PERMISSION);

        assertThat(result.kind()).isEqualTo(EmployeeDataScope.Kind.ALL);
        assertThat(result.storeId()).isNull();
        assertThat(result.storeCode()).isNull();
        assertThat(result.storeName()).isNull();
        assertThat(result.permissionCode()).isEqualTo(PERMISSION);
        verifyNoInteractions(storeStaffRepository, storeRepository);
    }

    @Test
    void superAdminResolvesGlobalWithoutLoadingAssignments() {
        allow();
        when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("SUPER_ADMIN"));

        EmployeeDataScope result = service.resolve(actorId, PERMISSION);

        assertThat(result).matches(EmployeeDataScope::isGlobal);
        assertThat(result.storeId()).isNull();
        verifyNoInteractions(storeStaffRepository, storeRepository);
    }

    @Test
    void nonAdminWithoutAssignmentResolvesGlobal() {
        allow();
        when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId)).thenReturn(List.of());

        EmployeeDataScope result = service.resolve(actorId, PERMISSION);

        assertThat(result.kind()).isEqualTo(EmployeeDataScope.Kind.ALL);
        assertThat(result.permissionCode()).isEqualTo(PERMISSION);
        verifyNoInteractions(storeRepository);
    }

    @Test
    void nonAdminWithOneActiveStoreResolvesThatStore() {
        allow();
        when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId)).thenReturn(List.of(assignment(storeId)));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore(storeId, "A", "Store A")));

        EmployeeDataScope result = service.resolve(actorId, PERMISSION);

        assertThat(result.kind()).isEqualTo(EmployeeDataScope.Kind.STORE);
        assertThat(result.storeId()).isEqualTo(storeId);
        assertThat(result.storeCode()).isEqualTo("A");
        assertThat(result.storeName()).isEqualTo("Store A");
        assertThat(result.permissionCode()).isEqualTo(PERMISSION);
    }

    @Test
    void duplicateActiveAssignmentsForSameStoreResolveThatStore() {
        allow();
        when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId))
                .thenReturn(List.of(assignment(storeId), assignment(storeId)));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore(storeId, "A", "Store A")));

        EmployeeDataScope result = service.resolve(actorId, PERMISSION);

        assertThat(result.storeId()).isEqualTo(storeId);
        assertThat(result.kind()).isEqualTo(EmployeeDataScope.Kind.STORE);
    }

    @Test
    void activeAssignmentsForDifferentStoresAreForbiddenAsAmbiguousConfiguration() {
        allow();
        when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId))
                .thenReturn(List.of(assignment(storeId), assignment(otherStoreId)));

        assertThatThrownBy(() -> service.resolve(actorId, PERMISSION))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("nhiều cửa hàng");
        verify(storeRepository, never()).findById(storeId);
    }

    @Test
    void missingPermissionIsForbiddenEvenForInternalCaller() {
        when(authorizationService.getPermissionScope(actorId, PERMISSION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(actorId, PERMISSION))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
        verifyNoInteractions(roleRepository, storeStaffRepository, storeRepository);
    }

    @Test
    void missingOrInactiveResolvedStoreIsForbidden() {
        allow();
        when(roleRepository.findCodesByUserId(actorId)).thenReturn(List.of("MANAGER"));
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(actorId)).thenReturn(List.of(assignment(storeId)));
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(actorId, PERMISSION))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(inactiveStore(storeId)));

        assertThatThrownBy(() -> service.resolve(actorId, PERMISSION))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void globalFilterKeepsRequestedStoreAndStoreFilterUsesItsOwnStore() {
        EmployeeDataScope global = EmployeeDataScope.all(PERMISSION);
        EmployeeDataScope scoped = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), PERMISSION);

        assertThat(service.validateFilter(global, null)).isNull();
        assertThat(service.validateFilter(global, otherStoreId)).isEqualTo(otherStoreId);
        assertThat(service.validateFilter(scoped, null)).isEqualTo(storeId);
        assertThat(service.validateFilter(scoped, storeId)).isEqualTo(storeId);
    }

    @Test
    void storeFilterRejectsAnotherStore() {
        EmployeeDataScope scoped = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), PERMISSION);

        assertThatThrownBy(() -> service.validateFilter(scoped, otherStoreId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void storeScopeRequiresTargetInSameActiveStoreAndGlobalSkipsTargetLookup() {
        EmployeeDataScope scoped = EmployeeDataScope.store(activeStore(storeId, "A", "Store A"), PERMISSION);
        EmployeeDataScope global = EmployeeDataScope.all(PERMISSION);
        UUID targetUserId = UUID.randomUUID();

        when(storeStaffRepository.existsByUserIdAndStoreIdAndActiveTrue(targetUserId, storeId)).thenReturn(true);
        service.requireTarget(scoped, targetUserId);

        when(storeStaffRepository.existsByUserIdAndStoreIdAndActiveTrue(targetUserId, storeId)).thenReturn(false);
        assertThatThrownBy(() -> service.requireTarget(scoped, targetUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        service.requireTarget(global, targetUserId);
        verify(storeStaffRepository, org.mockito.Mockito.times(2))
                .existsByUserIdAndStoreIdAndActiveTrue(targetUserId, storeId);
        verifyNoMoreInteractions(storeStaffRepository);
    }

    private void allow() {
        when(authorizationService.getPermissionScope(actorId, PERMISSION)).thenReturn(Optional.of(PermissionScope.STORE));
    }

    private StoreStaff assignment(UUID assignedStoreId) {
        return StoreStaff.builder().userId(actorId).storeId(assignedStoreId).active(true).build();
    }

    private Store activeStore(UUID id, String code, String name) {
        return Store.builder().id(id).code(code).name(name).active(true).build();
    }

    private Store inactiveStore(UUID id) {
        return Store.builder().id(id).code("A").name("Store A").active(false).build();
    }
}
