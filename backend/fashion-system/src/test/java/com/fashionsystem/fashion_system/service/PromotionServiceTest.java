package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PromotionMapper;
import com.fashionsystem.fashion_system.mapper.PromotionUsageMapper;
import com.fashionsystem.fashion_system.repository.CustomerRepository;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionUsageRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromotionServiceTest {
    @Test
    void capsPercentageDiscountAtConfiguredMaximum() {
        PromotionRepository repository = mock(PromotionRepository.class);
        UUID id = UUID.randomUUID();
        Promotion promotion = Promotion.builder().id(id).active(true).discountType("PERCENT")
                .discountValue(new BigDecimal("20")).maxDiscount(new BigDecimal("150"))
                .startDate(LocalDateTime.now().minusDays(1)).endDate(LocalDateTime.now().plusDays(1)).build();
        when(repository.findById(id)).thenReturn(Optional.of(promotion));
        PromotionService service = new PromotionService(
                repository, mock(PromotionUsageRepository.class), mock(PromotionMapper.class));

        BigDecimal discount = service.calculateDiscount(id, new BigDecimal("1000"), LocalDateTime.now());

        assertThat(discount).isEqualByComparingTo("150.00");
    }

    @Test
    void rejectsUsageWhenGlobalLimitHasBeenReached() {
        PromotionRepository promotionRepository = mock(PromotionRepository.class);
        PromotionUsageRepository usageRepository = mock(PromotionUsageRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UUID promotionId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Promotion promotion = Promotion.builder().id(promotionId).active(true).usageLimit(2).build();
        when(promotionRepository.findByIdForUpdate(promotionId)).thenReturn(Optional.of(promotion));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(Order.builder().id(orderId).build()));
        when(usageRepository.countByPromotionId(promotionId)).thenReturn(2L);
        PromotionUsageService service = new PromotionUsageService(
                promotionRepository, usageRepository, orderRepository,
                mock(CustomerRepository.class), mock(PromotionUsageMapper.class));

        assertThatThrownBy(() -> service.recordUsage(promotionId, orderId, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hết lượt");
        verify(usageRepository, never()).save(any());
    }
}
