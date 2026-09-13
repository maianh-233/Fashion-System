package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Store;
import com.fashionsystem.fashion_system.entity.StoreStaff;
import com.fashionsystem.fashion_system.exception.BusinessException;
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

/** Catches identity-scope bypasses caused by trusting RBAC scope or request Store ids. */
@ExtendWith(MockitoExtension.class)
class UserScopeServiceTest {
    @Mock StoreStaffRepository storeStaffRepository;
    @Mock StoreRepository storeRepository;

    private UserScopeService service;
    private UUID userId;
    private UUID storeId;
    private UUID otherStoreId;

    @BeforeEach
    void setUp() {
        service = new UserScopeService(storeStaffRepository, storeRepository);
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        otherStoreId = UUID.randomUUID();
    }

    @Test
    void userWithoutActiveStoreAssignmentIsGlobal() {
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        UserScope result = service.resolve(userId);

        assertThat(result.kind()).isEqualTo(UserScope.Kind.GLOBAL);
        assertThat(result.storeId()).isNull();
        verifyNoInteractions(storeRepository);
    }

    @Test
    void userWithOneActiveStoreIsStoreScoped() {
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(assignment(storeId)));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore(storeId)));

        UserScope result = service.resolve(userId);

        assertThat(result.kind()).isEqualTo(UserScope.Kind.STORE);
        assertThat(result.storeId()).isEqualTo(storeId);
        assertThat(result.storeCode()).isEqualTo("SGC");
        assertThat(result.storeName()).isEqualTo("Saigon Centre");
    }

    @Test
    void duplicateAssignmentsForSameStoreStillResolveOneStore() {
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(assignment(storeId), assignment(storeId)));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore(storeId)));

        assertThat(service.resolve(userId).storeId()).isEqualTo(storeId);
    }

    @Test
    void assignmentsForDifferentStoresAreForbiddenAsAmbiguous() {
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(assignment(storeId), assignment(otherStoreId)));

        assertThatThrownBy(() -> service.resolve(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasMessageContaining("nhiều cửa hàng");
        verify(storeRepository, never()).findById(storeId);
    }

    @Test
    void inactiveAssignedStoreIsForbidden() {
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(assignment(storeId)));
        when(storeRepository.findById(storeId))
                .thenReturn(Optional.of(Store.builder().id(storeId).active(false).build()));

        assertThatThrownBy(() -> service.resolve(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void storeUserCannotRequestAnotherStore() {
        stubStoreUser();

        assertThatThrownBy(() -> service.resolveStoreId(userId, otherStoreId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void storeUserRequestWithoutStoreIsForcedToOwnStore() {
        stubStoreUser();

        assertThat(service.resolveStoreId(userId, null)).isEqualTo(storeId);
        assertThat(service.resolveStoreId(userId, storeId)).isEqualTo(storeId);
    }

    @Test
    void globalUserMayKeepOptionalStoreFilter() {
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        assertThat(service.resolveStoreId(userId, null)).isNull();
        assertThat(service.resolveStoreId(userId, otherStoreId)).isEqualTo(otherStoreId);
    }

    @Test
    void storeAccessChecksPersistedStoreOwnership() {
        stubStoreUser();

        service.requireStoreAccess(userId, storeId);
        assertThatThrownBy(() -> service.requireStoreAccess(userId, otherStoreId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void storeUserCannotPassGlobalRequirement() {
        stubStoreUser();

        assertThatThrownBy(() -> service.requireGlobal(userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    private void stubStoreUser() {
        when(storeStaffRepository.findAllByUserIdAndActiveTrue(userId))
                .thenReturn(List.of(assignment(storeId)));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore(storeId)));
    }

    private StoreStaff assignment(UUID assignedStoreId) {
        return StoreStaff.builder().userId(userId).storeId(assignedStoreId).active(true).build();
    }

    private Store activeStore(UUID id) {
        return Store.builder().id(id).code("SGC").name("Saigon Centre").active(true).build();
    }
}
