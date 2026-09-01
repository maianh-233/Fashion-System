package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.InventoryBalanceDto;
import com.fashionsystem.fashion_system.entity.InventoryBalance;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.InventoryBalanceMapper;
import com.fashionsystem.fashion_system.mapper.InventoryTransactionMapper;
import com.fashionsystem.fashion_system.repository.InventoryBalanceRepository;
import com.fashionsystem.fashion_system.repository.InventoryTransactionRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
    @Test
    void receiveUpdatesLockedBalanceAndAppendsTransaction() {
        Fixture fixture = new Fixture(4, 2);
        InventoryBalanceDto response = InventoryBalanceDto.builder().availableQuantity(7).build();
        when(fixture.balanceMapper.toDto(fixture.balance)).thenReturn(response);

        InventoryBalanceDto result = fixture.service.receive(
                fixture.storeId, fixture.variantId, 3, UUID.randomUUID(), UUID.randomUUID());

        assertThat(result.getAvailableQuantity()).isEqualTo(7);
        assertThat(fixture.balance.getAvailableQuantity()).isEqualTo(7);
        verify(fixture.balanceRepository).save(fixture.balance);
        verify(fixture.transactionRepository).save(any());
    }

    @Test
    void issueRejectsQuantityGreaterThanAvailableWithoutWritingLedger() {
        Fixture fixture = new Fixture(2, 0);

        assertThatThrownBy(() -> fixture.service.issue(
                        fixture.storeId, fixture.variantId, 3, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không đủ");
        verify(fixture.balanceRepository, never()).save(any());
        verify(fixture.transactionRepository, never()).save(any());
    }

    private static final class Fixture {
        private final UUID storeId = UUID.randomUUID();
        private final UUID variantId = UUID.randomUUID();
        private final InventoryBalanceRepository balanceRepository = mock(InventoryBalanceRepository.class);
        private final InventoryTransactionRepository transactionRepository = mock(InventoryTransactionRepository.class);
        private final StoreRepository storeRepository = mock(StoreRepository.class);
        private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
        private final InventoryBalanceMapper balanceMapper = mock(InventoryBalanceMapper.class);
        private final InventoryBalance balance;
        private final InventoryService service;

        private Fixture(int available, int reserved) {
            balance = InventoryBalance.builder().storeId(storeId).productVariantId(variantId)
                    .availableQuantity(available).reservedQuantity(reserved).damagedQuantity(0).build();
            when(storeRepository.existsById(storeId)).thenReturn(true);
            when(variantRepository.existsById(variantId)).thenReturn(true);
            when(balanceRepository.findForUpdate(storeId, variantId)).thenReturn(Optional.of(balance));
            service = new InventoryService(balanceRepository, transactionRepository, storeRepository,
                    variantRepository, balanceMapper, mock(InventoryTransactionMapper.class));
        }
    }
}
