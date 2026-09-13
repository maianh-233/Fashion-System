package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.InventoryBalanceDto;
import com.fashionsystem.fashion_system.dto.InventoryStatisticsDto;
import com.fashionsystem.fashion_system.dto.StoreInventoryStatisticsDto;
import com.fashionsystem.fashion_system.entity.InventoryBalance;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.InventoryBalanceMapper;
import com.fashionsystem.fashion_system.mapper.InventoryTransactionMapper;
import com.fashionsystem.fashion_system.repository.InventoryBalanceRepository;
import com.fashionsystem.fashion_system.repository.InventoryTransactionRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Covers database-scoped Inventory reads and locked stock mutations. */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock InventoryBalanceRepository balanceRepository;
    @Mock InventoryTransactionRepository transactionRepository;
    @Mock StoreRepository storeRepository;
    @Mock ProductVariantRepository variantRepository;
    @Mock InventoryBalanceMapper balanceMapper;
    @Mock InventoryTransactionMapper transactionMapper;
    @Mock AuthorizationService authorizationService;
    @Mock UserScopeService userScopeService;

    private InventoryService service;
    private UUID actorId;
    private UUID storeA;
    private UUID storeB;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        service = new InventoryService(balanceRepository, transactionRepository, storeRepository,
                variantRepository, balanceMapper, transactionMapper,
                authorizationService, userScopeService);
        actorId = UUID.randomUUID();
        storeA = UUID.randomUUID();
        storeB = UUID.randomUUID();
        variantId = UUID.randomUUID();
    }

    @Test
    void storeListUsesResolvedStoreInRepositorySoTotalIsScoped() {
        var pageable = PageRequest.of(0, 20);
        InventoryBalance balance = balance(storeA, 4, 0);
        when(authorizationService.hasPermission(actorId, "INVENTORY_VIEW")).thenReturn(true);
        when(userScopeService.resolveStoreId(actorId, null)).thenReturn(storeA);
        when(balanceRepository.search(storeA, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(balance), pageable, 1));
        when(balanceMapper.toDto(balance)).thenReturn(
                InventoryBalanceDto.builder().storeId(storeA).availableQuantity(4).build());

        var result = service.getBalances(actorId, null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).allMatch(item -> item.getStoreId().equals(storeA));
        verify(balanceRepository).search(storeA, null, null, pageable);
    }

    @Test
    void directBalanceReadChecksPersistedStoreBeforeRepositoryLookup() {
        when(authorizationService.hasPermission(actorId, "INVENTORY_VIEW")).thenReturn(true);
        org.mockito.Mockito.doThrow(BusinessException.forbidden("Bạn không có quyền truy cập cửa hàng này"))
                .when(userScopeService).requireStoreAccess(actorId, storeB);

        assertThatThrownBy(() -> service.getBalance(actorId, storeB, variantId))
                .isInstanceOf(BusinessException.class);

        verify(balanceRepository, never()).findById(any());
    }

    @Test
    void globalListKeepsRequestedStoreFilter() {
        var pageable = PageRequest.of(0, 20);
        when(authorizationService.hasPermission(actorId, "INVENTORY_VIEW")).thenReturn(true);
        when(userScopeService.resolveStoreId(actorId, storeB)).thenReturn(storeB);
        when(balanceRepository.search(storeB, variantId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getBalances(actorId, storeB, variantId, null, pageable);

        verify(balanceRepository).search(storeB, variantId, null, pageable);
    }

    @Test
    void manualAdjustmentUsesAuthenticatedActorAndLockedBalance() {
        InventoryBalance balance = balance(storeA, 4, 0);
        when(authorizationService.hasPermission(actorId, "INVENTORY_ADJUST")).thenReturn(true);
        when(userScopeService.resolveStoreId(actorId, storeA)).thenReturn(storeA);
        when(storeRepository.existsById(storeA)).thenReturn(true);
        when(variantRepository.existsById(variantId)).thenReturn(true);
        when(balanceRepository.findForUpdate(storeA, variantId)).thenReturn(Optional.of(balance));
        when(balanceMapper.toDto(balance)).thenReturn(
                InventoryBalanceDto.builder().storeId(storeA).availableQuantity(7).build());

        InventoryBalanceDto result = service.adjust(actorId, storeA, variantId, 3);

        assertThat(result.getAvailableQuantity()).isEqualTo(7);
        verify(transactionRepository).save(argThatTransaction(actorId, 3, "ADJUST"));
    }

    @Test
    void adjustmentCannotMakeAvailableQuantityNegative() {
        InventoryBalance balance = balance(storeA, 2, 0);
        when(authorizationService.hasPermission(actorId, "INVENTORY_ADJUST")).thenReturn(true);
        when(userScopeService.resolveStoreId(actorId, storeA)).thenReturn(storeA);
        when(storeRepository.existsById(storeA)).thenReturn(true);
        when(variantRepository.existsById(variantId)).thenReturn(true);
        when(balanceRepository.findForUpdate(storeA, variantId)).thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> service.adjust(actorId, storeA, variantId, -3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không đủ");
        verify(balanceRepository, never()).save(any());
    }

    @Test
    void statisticsUsesEffectiveStoreAndDatabaseAggregates() {
        StoreInventoryStatisticsDto totals = new StoreInventoryStatisticsDto(
                storeA, "Store A", 2L, 7L, 1L, 0L, 1L, 0L);
        when(authorizationService.hasPermission(actorId, "INVENTORY_VIEW")).thenReturn(true);
        when(userScopeService.resolveStoreId(actorId, null)).thenReturn(storeA);
        when(userScopeService.resolve(actorId)).thenReturn(new UserScope(
                UserScope.Kind.STORE, storeA, "A", "Store A"));
        when(balanceRepository.summarize(storeA, 5)).thenReturn(totals);

        InventoryStatisticsDto result = service.getStatistics(actorId, null, 5);

        assertThat(result.totals()).isEqualTo(totals);
        assertThat(result.byStore()).containsExactly(totals);
        verify(balanceRepository).summarize(storeA, 5);
        verify(balanceRepository, never()).summarizeByStore(eq(5));
    }

    @Test
    void receiveUpdatesLockedBalanceAndAppendsTransaction() {
        InventoryBalance balance = balance(storeA, 4, 2);
        when(storeRepository.existsById(storeA)).thenReturn(true);
        when(variantRepository.existsById(variantId)).thenReturn(true);
        when(balanceRepository.findForUpdate(storeA, variantId)).thenReturn(Optional.of(balance));
        when(balanceMapper.toDto(balance)).thenReturn(
                InventoryBalanceDto.builder().availableQuantity(7).build());

        InventoryBalanceDto result = service.receive(
                storeA, variantId, 3, UUID.randomUUID(), actorId);

        assertThat(result.getAvailableQuantity()).isEqualTo(7);
        assertThat(balance.getAvailableQuantity()).isEqualTo(7);
        verify(balanceRepository).save(balance);
        verify(transactionRepository).save(any());
    }

    private org.mockito.ArgumentMatcher<com.fashionsystem.fashion_system.entity.InventoryTransaction>
            transaction(UUID createdBy, int quantity, String type) {
        return value -> value.getCreatedBy().equals(createdBy)
                && value.getQuantity() == quantity
                && value.getTransactionType().equals(type);
    }

    private com.fashionsystem.fashion_system.entity.InventoryTransaction argThatTransaction(
            UUID createdBy, int quantity, String type) {
        return org.mockito.ArgumentMatchers.argThat(transaction(createdBy, quantity, type));
    }

    private InventoryBalance balance(UUID storeId, int available, int reserved) {
        return InventoryBalance.builder().storeId(storeId).productVariantId(variantId)
                .availableQuantity(available).reservedQuantity(reserved).damagedQuantity(0).build();
    }
}
