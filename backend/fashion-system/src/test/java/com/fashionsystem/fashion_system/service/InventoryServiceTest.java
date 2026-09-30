package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.repository.*;
import com.fashionsystem.fashion_system.mapper.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class InventoryServiceTest {
    final InventoryBalanceRepository balances = mock(InventoryBalanceRepository.class);
    final InventoryTransactionRepository movements = mock(InventoryTransactionRepository.class);
    final StoreRepository stores = mock(StoreRepository.class);
    final ProductVariantRepository variants = mock(ProductVariantRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final StoreAccessService access = mock(StoreAccessService.class);
    final UserScopeService scopes = mock(UserScopeService.class);
    final UUID actor = UUID.randomUUID(), store = UUID.randomUUID(), variant = UUID.randomUUID(), receipt = UUID.randomUUID();
    InventoryService service;
    InventoryBalance row;
    @BeforeEach void setup() {
        service = new InventoryService(balances,movements,stores,variants,products,new InventoryBalanceMapper(),new InventoryTransactionMapper(),access,scopes);
        row = InventoryBalance.builder().storeId(store).productVariantId(variant)
            .availableQuantity(5).onlineQuantity(10).reservedQuantity(0).damagedQuantity(0).build();
        when(access.require(any(),any(),anyString())).thenAnswer(i -> i.getArgument(1));
        when(stores.existsById(store)).thenReturn(true);
        when(variants.existsById(variant)).thenReturn(true);
        when(balances.findForUpdate(store,variant)).thenReturn(Optional.of(row));
    }
    @Test void transferConservesTotalAndRecordsBothBuckets() {
        service.exportChannel(store,variant,3,"ONLINE","ONLINE_TO_OFFLINE",receipt,actor);
        assertThat(row.getOnlineQuantity()).isEqualTo(7);
        assertThat(row.getOfflineQuantity()).isEqualTo(8);
        assertThat(row.getTotalQuantity()).isEqualTo(15);
        verify(movements).save(argThat(m -> m.getBeforeOnline()==10 && m.getAfterOnline()==7
            && m.getBeforeOffline()==5 && m.getAfterOffline()==8 && m.getCreatedBy().equals(actor)));
    }
    @Test void insufficientOfflineDoesNotUseOnlineStock() {
        assertThatThrownBy(() -> service.exportChannel(store,variant,6,"OFFLINE","DAMAGED",receipt,actor)).isInstanceOf(BusinessException.class);
        assertThat(row.getOfflineQuantity()).isEqualTo(5);
        verify(movements,never()).save(any());
    }
    @Test void importOnlyIncreasesTargetBucket() {
        service.receiveChannel(store,variant,4,"ONLINE",receipt,actor);
        assertThat(row.getOnlineQuantity()).isEqualTo(14);
        assertThat(row.getOfflineQuantity()).isEqualTo(5);
    }
    @Test void returnReducesAvailableTotal() {
        service.exportChannel(store,variant,4,"OFFLINE","RETURN_TO_SUPPLIER",receipt,actor);
        assertThat(row.getTotalQuantity()).isEqualTo(11);
    }
    @Test void successiveExportsCannotOversellSameLockedRow() {
        service.exportChannel(store,variant,8,"ONLINE","OTHER",receipt,actor);
        assertThatThrownBy(() -> service.exportChannel(store,variant,5,"ONLINE","OTHER",UUID.randomUUID(),actor)).isInstanceOf(BusinessException.class);
        assertThat(row.getOnlineQuantity()).isEqualTo(2);
    }
    @Test void overflowRejectedBeforeMutation() {
        assertThatThrownBy(() -> service.receiveChannel(store,variant,Integer.MAX_VALUE,"ONLINE",receipt,actor)).isInstanceOf(BusinessException.class);
        assertThat(row.getOnlineQuantity()).isEqualTo(10);
    }
    @Test void invalidTransferSourceRejected() {
        assertThatThrownBy(() -> service.exportChannel(store,variant,1,"OFFLINE","ONLINE_TO_OFFLINE",receipt,actor)).isInstanceOf(BusinessException.class);
    }
    @Test void consumptionRecordsRemovedReservedQuantity() {
        row.setReservedQuantity(4);
        service.consumeReservation(store,variant,3,receipt,actor);
        assertThat(row.getReservedQuantity()).isEqualTo(1);
        verify(movements).save(argThat(m -> m.getQuantity()==-3 && m.getTransactionType().equals("RESERVATION_CONSUMED")));
    }
    @Test void unauthorizedMutationRejectedBeforeLock() {
        doThrow(BusinessException.forbidden("denied")).when(access).require(actor,store,"EXPORT_RECEIPT_COMPLETE");
        assertThatThrownBy(() -> service.exportChannel(store,variant,1,"ONLINE","DAMAGED",receipt,actor)).isInstanceOf(BusinessException.class);
        verify(balances,never()).findForUpdate(any(),any());
    }
}
