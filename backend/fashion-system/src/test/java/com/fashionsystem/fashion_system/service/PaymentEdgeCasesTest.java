package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderItem;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.RefundMapper;
import com.fashionsystem.fashion_system.payment.PaymentGateway;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PaymentTransactionRepository;
import com.fashionsystem.fashion_system.repository.RefundRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentEdgeCasesTest {
    final RefundRepository refunds = mock(RefundRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final OrderRepository orders = mock(OrderRepository.class);
    final LoyaltyEarningService loyalty = mock(LoyaltyEarningService.class);
    final RefundService service = new RefundService(refunds, payments, mock(PaymentTransactionRepository.class), orders,
            mock(UserRepository.class), mock(RefundMapper.class), loyalty);
    final UUID refundId = UUID.randomUUID(), paymentId = UUID.randomUUID(), orderId = UUID.randomUUID();
    final Order order = Order.builder().id(orderId).paymentStatus("PAID").build();

    Refund pending(boolean duplicate) {
        Refund refund = Refund.builder().id(refundId).paymentId(paymentId).amount(BigDecimal.valueOf(100_000)).status("PENDING")
                .duplicatePayment(duplicate).build();
        when(refunds.findByIdForUpdate(refundId)).thenReturn(Optional.of(refund));
        when(payments.findByIdForUpdate(paymentId)).thenReturn(Optional.of(Payment.builder().id(paymentId).orderId(orderId)
                .amount(BigDecimal.valueOf(100_000)).status("SUCCESS").build()));
        when(refunds.sumCompletedAmount(paymentId)).thenReturn(BigDecimal.valueOf(100_000));
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        return refund;
    }

    @Test
    void refundingADuplicatePaymentLeavesTheOrderPaidAndKeepsPoints() {
        Refund refund = pending(true);
        service.complete(refundId, "GD1", null);
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        verify(loyalty, never()).reverseForRefund(any(), any());
        assertThat(refund.getRefundCode()).startsWith("HT-");
    }

    @Test
    void fullRefundMarksTheOrderAndItsPartsRefundedAndReversesPoints() {
        Order child = Order.builder().id(UUID.randomUUID()).paymentStatus("PAID").build();
        when(orders.findAllByParentOrderId(orderId)).thenReturn(List.of(child));
        pending(false);
        service.complete(refundId, "GD1", null);
        assertThat(order.getPaymentStatus()).isEqualTo("REFUNDED");
        assertThat(child.getPaymentStatus()).isEqualTo("REFUNDED");
        verify(loyalty).reverseForRefund(any(), any());
    }

    @Test
    void rejectionKeepsTheReason() {
        Refund refund = pending(false);
        service.reject(refundId, " Khách đã nhận lại tiền ");
        assertThat(refund.getStatus()).isEqualTo("REJECTED");
        assertThat(refund.getRejectReason()).isEqualTo("Khách đã nhận lại tiền");
    }

    OrderItem line(long total, int quantity) {
        return OrderItem.builder().id(UUID.randomUUID()).price(BigDecimal.valueOf(total / quantity)).quantity(quantity)
                .total(BigDecimal.valueOf(total)).build();
    }

    @Test
    void returnPriceSharesTheOrderVoucherAcrossLines() {
        OrderAmountCalculator amounts = new OrderAmountCalculator(orders);
        Order paid = Order.builder().id(orderId).totalAmount(BigDecimal.valueOf(800_000)).build();
        OrderItem a = line(500_000, 1), b = line(500_000, 1);
        var prices = amounts.paidUnitPrices(paid, List.of(a, b));
        assertThat(prices.get(a.getId())).isEqualByComparingTo("400000");
        assertThat(prices.get(b.getId())).isEqualByComparingTo("400000");
    }

    @Test
    void returnPricesNeverAddUpToMoreThanThePaidAmount() {
        OrderAmountCalculator amounts = new OrderAmountCalculator(orders);
        Order paid = Order.builder().id(orderId).totalAmount(BigDecimal.valueOf(100_000)).build();
        OrderItem a = line(100_000, 3), b = line(50_001, 7);
        var prices = amounts.paidUnitPrices(paid, List.of(a, b));
        BigDecimal refundable = prices.get(a.getId()).multiply(BigDecimal.valueOf(3)).add(prices.get(b.getId()).multiply(BigDecimal.valueOf(7)));
        assertThat(refundable).isLessThanOrEqualTo(BigDecimal.valueOf(100_000));
    }

    @Test
    void orderWithoutVoucherRefundsTheLinePrice() {
        OrderAmountCalculator amounts = new OrderAmountCalculator(orders);
        Order paid = Order.builder().id(orderId).totalAmount(BigDecimal.valueOf(600_000)).build();
        OrderItem a = line(600_000, 2);
        assertThat(amounts.paidUnitPrices(paid, List.of(a)).get(a.getId())).isEqualByComparingTo("300000");
    }

    @Test
    void amountsOutsideTheGatewayLimitAreRefused() {
        PaymentGateway gateway = new PaymentGateway() {
            @Override public String code() { return "VNPAY"; }
            @Override public boolean configured() { return true; }
            @Override public long minAmount() { return 5_000; }
            @Override public long maxAmount() { return 1_000_000; }
            @Override public com.fashionsystem.fashion_system.payment.PaymentModels.PaymentLink createPayment(
                    com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest request) { return null; }
            @Override public com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback parseCallback(
                    com.fashionsystem.fashion_system.payment.PaymentModels.CallbackInput input) { return null; }
        };
        gateway.requireSupportedAmount(BigDecimal.valueOf(5_000));
        assertThatThrownBy(() -> gateway.requireSupportedAmount(BigDecimal.ZERO)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("thanh toán khi nhận hàng");
        assertThatThrownBy(() -> gateway.requireSupportedAmount(BigDecimal.valueOf(1_000_001))).isInstanceOf(BusinessException.class);
    }
}
