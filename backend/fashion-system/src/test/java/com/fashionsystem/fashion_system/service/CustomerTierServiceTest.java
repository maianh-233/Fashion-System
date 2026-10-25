package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.CustomerTierDto;
import com.fashionsystem.fashion_system.entity.CustomerTier;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.CustomerTierMapper;
import com.fashionsystem.fashion_system.repository.CustomerTierAssignmentRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTierServiceTest {
    final CustomerTierRepository tiers = mock(CustomerTierRepository.class);
    final CustomerTierAssignmentRepository assignments = mock(CustomerTierAssignmentRepository.class);
    final CustomerAccessService access = mock(CustomerAccessService.class);
    final CustomerTierService service = new CustomerTierService(tiers, assignments, mock(CustomerTierMapper.class),
            mock(CatalogReadCacheService.class), access);
    final UUID actor = UUID.randomUUID();
    final UUID regularId = UUID.randomUUID();

    CustomerTierServiceTest() {
        when(tiers.findById(regularId)).thenReturn(Optional.of(
                CustomerTier.builder().id(regularId).code("REGULAR").minTotalSpent(BigDecimal.ZERO).build()));
    }

    private static CustomerTierDto tier(String code, String minSpent) {
        return CustomerTierDto.builder().code(code).name(code).minTotalSpent(new BigDecimal(minSpent))
                .discountPercent(BigDecimal.ZERO).build();
    }

    @Test
    void configurationRequiresTierManager() {
        doThrow(BusinessException.forbidden("no")).when(access).requireTierManager(actor);

        assertThatThrownBy(() -> service.create(actor, tier("GOLD", "1000"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.delete(actor, regularId)).isInstanceOf(BusinessException.class);
        verify(tiers, never()).save(any());
        verify(tiers, never()).deleteById(any());
    }

    @Test
    void defaultTierCannotBeDeletedRenamedOrGivenAThreshold() {
        assertThatThrownBy(() -> service.delete(actor, regularId))
                .isInstanceOf(BusinessException.class).hasMessageContaining("REGULAR");
        assertThatThrownBy(() -> service.update(actor, regularId, tier("BASIC", "0")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("mã");
        assertThatThrownBy(() -> service.update(actor, regularId, tier("REGULAR", "500000")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ngưỡng");
        verify(tiers, never()).deleteById(any());
        verify(tiers, never()).save(any());
    }
}
