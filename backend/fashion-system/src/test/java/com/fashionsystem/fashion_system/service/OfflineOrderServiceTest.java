package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.OfflineCartRequest;
import com.fashionsystem.fashion_system.dto.order.PosVariantRow;
import com.fashionsystem.fashion_system.entity.*;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OfflineOrderServiceTest {
    final OrderAccessService access = mock(OrderAccessService.class);
    final PosCatalogRepository catalog = mock(PosCatalogRepository.class);
    final OrderVoucherService vouchers = mock(OrderVoucherService.class);
    final PromotionRepository promotions = mock(PromotionRepository.class);
    final CustomerRepository customers = mock(CustomerRepository.class);
    final CustomerPromotionRepository customerPromotions = mock(CustomerPromotionRepository.class);
    final PromotionUsageRepository usages = mock(PromotionUsageRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final OrderItemRepository items = mock(OrderItemRepository.class);
    final OrderPromotionRepository orderPromotions = mock(OrderPromotionRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final InventoryService inventory = mock(InventoryService.class);
    final OrderCodeGenerator codes = mock(OrderCodeGenerator.class);
    final OrderStatusService statuses = new OrderStatusService(orders, mock(OrderStatusHistoryRepository.class),
            new OrderStateMachine(), mock(org.springframework.context.ApplicationEventPublisher.class));
    final OrderCartPricer pricer = new OrderCartPricer(vouchers, new OrderPricingCalculator(), promotions, customers,
            customerPromotions, usages, orderPromotions);
    final OfflineOrderService service = new OfflineOrderService(access, catalog, pricer, orders, items, payments,
            inventory, codes, statuses);

    final UUID actor = UUID.randomUUID(), store = UUID.randomUUID(), orderId = UUID.randomUUID();
    final UUID productA = UUID.randomUUID(), productB = UUID.randomUUID();
    final PosVariantRow shirt = row("SHIRT-M", productA, 300_000, 5);
    final PosVariantRow jeans = row("JEANS-30", productB, 500_000, 1);

    PosVariantRow row(String sku, UUID product, long price, int stock) {
        return new PosVariantRow(UUID.randomUUID(), product, sku + " name", "PRD", null, sku, null, "Đen", "M",
                BigDecimal.valueOf(price), null, true, "ACTIVE", stock);
    }

    OfflineCartRequest cart(long cash, List<String> codes, OfflineCartRequest.Line... lines) {
        return new OfflineCartRequest(null, null, List.of(lines), codes, BigDecimal.valueOf(cash), null);
    }

    OfflineCartRequest.Line line(PosVariantRow row, int quantity) {
        return new OfflineCartRequest.Line(row.variantId(), quantity);
    }

    @BeforeEach void setUp() {
        when(access.resolveStore(eq(actor), any(), eq(OrderPermissions.CREATE_OFFLINE))).thenReturn(store);
        when(catalog.findByIds(eq(store), any())).thenReturn(List.of(shirt, jeans));
        when(vouchers.targets(any())).thenReturn(new OrderVoucherService.Targets(Map.of(), Map.of()));
        when(codes.nextCode()).thenReturn("LB-261014-000001");
        when(orders.save(any())).thenAnswer(call -> {
            Order order = call.getArgument(0);
            if (order.getId() == null) order.setId(orderId);
            return order;
        });
    }

    @Test void quoteFlagsOverStockAndInsufficientCashWithoutBlockingTheCall() {
        var quote = service.quote(actor, cart(500_000, List.of(), line(shirt, 1), line(jeans, 2)));

        assertThat(quote.subtotal()).isEqualByComparingTo("1300000");
        assertThat(quote.warnings()).anyMatch(w -> w.contains("JEANS-30 chỉ còn 1"))
                .anyMatch(w -> w.contains("Tiền khách đưa còn thiếu"));
        assertThat(quote.lines().get(1).warning()).contains("chỉ còn 1");
        assertThat(quote.canCreate()).isFalse();
        assertThat(quote.changeAmount()).isNull();
    }

    @Test void createRecalculatesDeductsStockRecordsVoucherAndCompletesOrder() {
        Promotion order10 = Promotion.builder().id(UUID.randomUUID()).code("KM_O10").promotionType(PromotionType.ORDER)
                .discountType("PERCENT").discountValue(BigDecimal.TEN).maxDiscount(BigDecimal.valueOf(100_000))
                .status(PromotionStatus.ACTIVE).build();
        when(promotions.findAllByCodeIn(Set.of("KM_O10"))).thenReturn(List.of(order10));
        when(promotions.findByIdForUpdate(order10.getId())).thenReturn(Optional.of(order10));
        when(vouchers.ineligibleReason(eq(order10), any(), any(), any())).thenReturn(null);

        var created = service.create(actor, cart(1_000_000, List.of("km_o10"), line(shirt, 2), line(jeans, 1)));

        // 1.100.000 - 10% (tối đa 100.000) = 1.000.000; khách đưa đủ, tiền thối 0.
        assertThat(created.orderCode()).isEqualTo("LB-261014-000001");
        assertThat(created.total()).isEqualByComparingTo("1000000");
        assertThat(created.changeAmount()).isEqualByComparingTo("0");

        var saved = ArgumentCaptor.forClass(Order.class);
        verify(orders).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(saved.getValue().getOrderType()).isEqualTo(OrderType.OFFLINE);
        assertThat(saved.getValue().getHandlerUserId()).isEqualTo(actor);
        assertThat(saved.getValue().getDiscountTotal()).isEqualByComparingTo("100000");

        verify(promotions).findByIdForUpdate(order10.getId());
        verify(usages).save(argThat(u -> u.getPromotionId().equals(order10.getId()) && u.getOrderId().equals(orderId)));
        verify(inventory).deductForOrder(store, shirt.variantId(), 2, "OFFLINE", orderId, actor);
        verify(inventory).deductForOrder(store, jeans.variantId(), 1, "OFFLINE", orderId, actor);
        verify(payments).save(argThat(p -> "CASH".equals(p.getMethod()) && "SUCCESS".equals(p.getStatus())));
        verify(items, times(2)).save(any());
    }

    @Test void stockIsDeductedInVariantIdOrderToAvoidDeadlocks() {
        service.create(actor, cart(2_000_000, List.of(), line(jeans, 1), line(shirt, 1)));
        var first = shirt.variantId().compareTo(jeans.variantId()) < 0 ? shirt : jeans;
        var second = first == shirt ? jeans : shirt;
        InOrder order = inOrder(inventory);
        order.verify(inventory).deductForOrder(eq(store), eq(first.variantId()), anyInt(), any(), any(), any());
        order.verify(inventory).deductForOrder(eq(store), eq(second.variantId()), anyInt(), any(), any(), any());
    }

    @Test void createRejectsInsufficientCashAndSavesNothing() {
        assertThatThrownBy(() -> service.create(actor, cart(100_000, List.of(), line(shirt, 1))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Tiền khách đưa còn thiếu 200.000đ");
        verify(orders, never()).save(any());
        verifyNoInteractions(inventory);
    }

    @Test void createRejectsOverStockCart() {
        assertThatThrownBy(() -> service.create(actor, cart(5_000_000, List.of(), line(jeans, 3))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("chỉ còn 1");
        verify(orders, never()).save(any());
    }

    @Test void createRejectsIneligibleVoucherEvenIfFrontendSentIt() {
        Promotion limited = Promotion.builder().id(UUID.randomUUID()).code("KM_LIMIT").promotionType(PromotionType.ORDER)
                .discountType("FIXED").discountValue(BigDecimal.valueOf(50_000)).status(PromotionStatus.ACTIVE).build();
        when(promotions.findAllByCodeIn(any())).thenReturn(List.of(limited));
        when(promotions.findByIdForUpdate(limited.getId())).thenReturn(Optional.of(limited));
        when(vouchers.ineligibleReason(eq(limited), any(), any(), any())).thenReturn("Voucher đã hết lượt sử dụng");

        assertThatThrownBy(() -> service.create(actor, cart(5_000_000, List.of("KM_LIMIT"), line(shirt, 1))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("hết lượt");
        verifyNoInteractions(usages, inventory);
    }

    @Test void pointVoucherIsMarkedUsedInsteadOfCountingUsage() {
        UUID customerId = UUID.randomUUID();
        when(customers.findById(customerId)).thenReturn(Optional.of(Customer.builder().id(customerId).active(true).build()));
        when(vouchers.customer(customerId)).thenReturn(new OrderVoucherService.CustomerContext(customerId, null, Set.of()));
        Promotion point = Promotion.builder().id(UUID.randomUUID()).code("KM_PT").promotionType(PromotionType.POINT_REDEMPTION)
                .discountType("FIXED").discountValue(BigDecimal.valueOf(50_000)).status(PromotionStatus.ACTIVE).build();
        when(promotions.findAllByCodeIn(any())).thenReturn(List.of(point));
        when(promotions.findByIdForUpdate(point.getId())).thenReturn(Optional.of(point));
        CustomerPromotion owned = CustomerPromotion.builder().id(UUID.randomUUID()).customerId(customerId)
                .promotionId(point.getId()).pointsSpent(100).build();
        when(customerPromotions.findUnusedForUpdate(customerId, point.getId())).thenReturn(List.of(owned));

        service.create(actor, new OfflineCartRequest(null, customerId, List.of(line(shirt, 1)), List.of("KM_PT"),
                BigDecimal.valueOf(300_000), null));

        assertThat(owned.getUsedOrderId()).isEqualTo(orderId);
        verify(usages, never()).save(any());
    }
}
