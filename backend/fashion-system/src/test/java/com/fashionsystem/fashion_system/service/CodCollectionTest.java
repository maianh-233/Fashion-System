package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CodCollectionTest {
    final OrderRepository orders = mock(OrderRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final OrderAmountCalculator amounts = new OrderAmountCalculator(orders);
    final CodCollectionService service = new CodCollectionService(payments, orders, amounts);
    final List<Payment> saved = new ArrayList<>();

    static BigDecimal vnd(long value) {
        return BigDecimal.valueOf(value);
    }

    Order order(String code, UUID parent, long total, long discount, long shipping) {
        return Order.builder().id(UUID.randomUUID()).orderCode(code).parentOrderId(parent).orderType(OrderType.ONLINE)
                .status(OrderStatus.COMPLETED).paymentMethod("COD").paymentStatus("UNPAID")
                .totalAmount(vnd(total)).discountTotal(vnd(discount)).shippingFee(vnd(shipping)).build();
    }

    @BeforeEach void setUp() {
        when(payments.save(any())).thenAnswer(call -> { saved.add(call.getArgument(0)); return call.getArgument(0); });
        when(payments.sumSuccessfulAmount(any())).thenAnswer(call -> saved.stream()
                .filter(p -> p.getOrderId().equals(call.getArgument(0))).map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test void allocationRoundsToDongAndKeepsTheTotal() {
        var shares = OrderAmountCalculator.allocate(vnd(100_000), List.of(vnd(1), vnd(1), vnd(1)));
        assertThat(shares).extracting(BigDecimal::intValue).containsExactly(33_333, 33_333, 33_334);
        assertThat(OrderAmountCalculator.allocate(vnd(0), List.of(vnd(5), vnd(5)))).allMatch(s -> s.signum() == 0);
    }

    @Test void singleStoreCodOrderIsMarkedPaidForItsTotal() {
        Order order = order("LB-1", null, 938_000, 30_000, 0);
        service.recordCollected(order, "LM-1");
        assertThat(saved).singleElement().satisfies(p -> {
            assertThat(p.getAmount()).isEqualByComparingTo("938000");
            assertThat(p.getPaymentCode()).isEqualTo("COD-LB-1");
            assertThat(p.getStatus()).isEqualTo("SUCCESS");
            assertThat(p.getTransactionCode()).isEqualTo("LM-1");
        });
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test void splitOrderPartsCollectTheirShareOfVoucherAndShipping() {
        // Đơn tổng: 2 phần 600.000 + 400.000 (đã trừ giảm giá sản phẩm 20.000 ở phần 1), voucher toàn đơn 50.000, ship 30.000.
        Order parent = order("LB-10", null, 980_000, 70_000, 30_000);
        Order first = order("LB-11", parent.getId(), 600_000, 20_000, 0);
        Order second = order("LB-12", parent.getId(), 400_000, 0, 0);
        when(orders.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(orders.findByIdForUpdate(parent.getId())).thenReturn(Optional.of(parent));
        when(orders.findAllByParentOrderId(parent.getId())).thenReturn(List.of(second, first));

        service.recordCollected(first, "LM-A");
        // 600.000 - 60% * 50.000 + 60% * 30.000 = 588.000
        assertThat(saved.getFirst().getAmount()).isEqualByComparingTo("588000");
        assertThat(saved.getFirst().getOrderId()).isEqualTo(parent.getId());
        assertThat(first.getPaymentStatus()).isEqualTo("PAID");
        assertThat(parent.getPaymentStatus()).isEqualTo("PARTIALLY_PAID");

        service.recordCollected(second, "LM-B");
        assertThat(saved.get(1).getAmount()).isEqualByComparingTo("392000");
        assertThat(parent.getPaymentStatus()).isEqualTo("PAID");
    }

    @Test void repeatedWebhookDoesNotCollectTwiceAndPrepaidOrdersAreSkipped() {
        Order order = order("LB-1", null, 100_000, 0, 0);
        when(payments.existsByPaymentCode("COD-LB-1")).thenReturn(true);
        service.recordCollected(order, "LM-1");
        Order prepaid = order("LB-2", null, 100_000, 0, 0);
        prepaid.setPaymentMethod("VNPAY");
        service.recordCollected(prepaid, "LM-2");
        assertThat(saved).isEmpty();
    }
}
