package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.StockReservationDto;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.StockReservationMapper;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import com.fashionsystem.fashion_system.repository.StockReservationRepository;
import com.fashionsystem.fashion_system.repository.StoreRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class StockReservationServiceTest {
    @Test void activeReservationUniqueViolationReturnsConflictBeforeStockMutation() {
        var reservations = mock(StockReservationRepository.class);
        var orders = mock(OrderRepository.class);
        var stores = mock(StoreRepository.class);
        var variants = mock(ProductVariantRepository.class);
        var inventory = mock(InventoryService.class);
        var service = new StockReservationService(
                reservations, orders, stores, variants, new StockReservationMapper(), inventory);
        UUID order = UUID.randomUUID(), store = UUID.randomUUID(), variant = UUID.randomUUID();
        var request = StockReservationDto.builder()
                .orderId(order).storeId(store).productVariantId(variant).quantity(2).build();
        when(orders.existsById(order)).thenReturn(true);
        when(stores.existsById(store)).thenReturn(true);
        when(variants.existsById(variant)).thenReturn(true);
        when(reservations.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("duplicate", new RuntimeException("uq_stock_reservations_active")));

        assertThatThrownBy(() -> service.create(request, UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(inventory);
    }
}
