package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionStatus;
import com.fashionsystem.fashion_system.entity.PromotionType;
import com.fashionsystem.fashion_system.repository.*;
import com.fashionsystem.fashion_system.service.OrderVoucherService.CustomerContext;
import com.fashionsystem.fashion_system.service.OrderVoucherService.Targets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderVoucherServiceTest {
    final PromotionUsageRepository usages = mock(PromotionUsageRepository.class);
    final OrderVoucherService service = new OrderVoucherService(mock(PromotionRepository.class),
            mock(PromotionProductRepository.class), mock(PromotionTierRepository.class), usages,
            mock(CustomerPromotionRepository.class), mock(CustomerTierRepository.class));
    final LocalDateTime now = LocalDateTime.of(2026, 10, 14, 10, 0);
    final UUID customerId = UUID.randomUUID(), gold = UUID.randomUUID();
    final Targets noTargets = new Targets(Map.of(), Map.of());

    Promotion promo(PromotionType type) {
        return Promotion.builder().id(UUID.randomUUID()).code("KM_TEST").promotionType(type).discountType("FIXED")
                .discountValue(BigDecimal.TEN).status(PromotionStatus.ACTIVE)
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build();
    }

    CustomerContext member(UUID tier, Set<UUID> owned) {
        return new CustomerContext(customerId, tier, owned);
    }

    @Test void activeUnlimitedVoucherIsUsableByWalkInCustomer() {
        assertThat(service.ineligibleReason(promo(PromotionType.ORDER), CustomerContext.walkIn(), noTargets, now)).isNull();
    }

    @Test void expiredOrInactiveVoucherIsRejected() {
        Promotion expired = promo(PromotionType.ORDER);
        expired.setEndDate(now);
        assertThat(service.ineligibleReason(expired, CustomerContext.walkIn(), noTargets, now)).contains("hiệu lực");
        Promotion inactive = promo(PromotionType.ORDER);
        inactive.setStatus(PromotionStatus.INACTIVE);
        assertThat(service.ineligibleReason(inactive, CustomerContext.walkIn(), noTargets, now)).contains("hiệu lực");
    }

    @Test void perCustomerLimitNeedsIdentifiedCustomerAndRemainingUses() {
        Promotion promotion = promo(PromotionType.ORDER);
        promotion.setUsagePerUser(2);
        assertThat(service.ineligibleReason(promotion, CustomerContext.walkIn(), noTargets, now)).contains("cần chọn khách");

        when(usages.countByPromotionIdAndCustomerId(promotion.getId(), customerId)).thenReturn(2L);
        assertThat(service.ineligibleReason(promotion, member(null, Set.of()), noTargets, now)).contains("hết lượt");
    }

    @Test void totalUsageLimitIsEnforced() {
        Promotion promotion = promo(PromotionType.PRODUCT);
        promotion.setUsageLimit(100);
        when(usages.countByPromotionId(promotion.getId())).thenReturn(100L);
        assertThat(service.ineligibleReason(promotion, CustomerContext.walkIn(), noTargets, now)).contains("hết lượt");
    }

    @Test void tierRestrictedVoucherOnlyForMembersOfThatTier() {
        Promotion promotion = promo(PromotionType.ORDER);
        Targets targets = new Targets(Map.of(), Map.of(promotion.getId(), Set.of(gold)));
        assertThat(service.ineligibleReason(promotion, CustomerContext.walkIn(), targets, now)).isNotNull();
        assertThat(service.ineligibleReason(promotion, member(UUID.randomUUID(), Set.of()), targets, now)).contains("Hạng");
        assertThat(service.ineligibleReason(promotion, member(gold, Set.of()), targets, now)).isNull();
    }

    @Test void pointVoucherRequiresAnUnusedRedeemedCopyButIgnoresRedemptionLimit() {
        Promotion promotion = promo(PromotionType.POINT_REDEMPTION);
        promotion.setUsageLimit(1);
        when(usages.countByPromotionId(promotion.getId())).thenReturn(5L);
        assertThat(service.ineligibleReason(promotion, member(null, Set.of()), noTargets, now)).contains("chưa đổi điểm");
        assertThat(service.ineligibleReason(promotion, member(null, Set.of(promotion.getId())), noTargets, now)).isNull();
    }
}
