package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.PaymentWebhookLog;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payment.PaymentGatewayRegistry;
import com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback;
import com.fashionsystem.fashion_system.payment.PaymentModels.Outcome;
import com.fashionsystem.fashion_system.payment.PaymentProperties;
import com.fashionsystem.fashion_system.payment.VnpayGateway;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PaymentTransactionRepository;
import com.fashionsystem.fashion_system.repository.PaymentWebhookLogRepository;
import com.fashionsystem.fashion_system.repository.RefundRepository;
import com.fashionsystem.fashion_system.service.OrderPaymentService.CallbackResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class OrderPaymentServiceTest {
    final OrderRepository orders = mock(OrderRepository.class);
    final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    final PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    final PaymentWebhookLogRepository logs = mock(PaymentWebhookLogRepository.class);
    final RefundRepository refunds = mock(RefundRepository.class);
    final OnlineOrderService onlineOrders = mock(OnlineOrderService.class);
    final OrderStatusService statusService = mock(OrderStatusService.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final PaymentProperties properties = new PaymentProperties(15, "https://shop.example/", null, 15,
            new PaymentProperties.Vnpay("TESTCODE", "SECRET", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"), null);
    final OrderPaymentService service = new OrderPaymentService(orders, paymentRepository, transactions, logs, refunds,
            onlineOrders, statusService, new PaymentGatewayRegistry(List.of(new VnpayGateway(properties))), properties, events);

    final UUID customerId = UUID.randomUUID();
    final Map<UUID, Payment> payments = new LinkedHashMap<>();
    final List<PaymentWebhookLog> savedLogs = new ArrayList<>();
    Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder().id(UUID.randomUUID()).orderCode("LB-261019-000001").orderType(OrderType.ONLINE)
                .storeId(UUID.randomUUID()).customerId(customerId).status(OrderStatus.PENDING).paymentMethod("VNPAY")
                .paymentStatus(OrderPaymentStatus.AWAITING_PAYMENT).paymentDueAt(LocalDateTime.now().plusMinutes(15))
                .totalAmount(new BigDecimal("125000.00")).build();
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(call -> {
            Payment payment = call.getArgument(0);
            if (payment.getId() == null) payment.setId(UUID.randomUUID());
            payments.put(payment.getId(), payment);
            return payment;
        });
        when(paymentRepository.findByIdForUpdate(any())).thenAnswer(call -> Optional.ofNullable(payments.get(call.getArgument(0))));
        when(paymentRepository.findByPaymentCode(anyString())).thenAnswer(call -> payments.values().stream()
                .filter(p -> p.getPaymentCode().equals(call.getArgument(0))).findFirst());
        when(paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId()))
                .thenAnswer(call -> new ArrayList<>(payments.values()));
        when(logs.save(any())).thenAnswer(call -> { savedLogs.add(call.getArgument(0)); return call.getArgument(0); });
        when(onlineOrders.storeOrders(order)).thenReturn(List.of(order));
        when(onlineOrders.placedEvent(order)).thenReturn(new OrderEvents.OnlineOrderPlaced(order.getId(),
                order.getOrderCode(), customerId, false, List.of()));
    }

    Payment pendingAttempt(String code) {
        return paymentRepository.save(Payment.builder().orderId(order.getId()).paymentCode(code).method("VNPAY")
                .amount(new BigDecimal("125000.00")).status("PENDING").expiresAt(order.getPaymentDueAt())
                .createdAt(LocalDateTime.now()).build());
    }

    GatewayCallback callback(String reference, Outcome outcome, String amount) {
        return new GatewayCallback(reference, "14567890", new BigDecimal(amount), outcome, "00/00", "ok",
                reference + ":14567890:" + outcome, "{}");
    }

    @Test
    void preparesPendingAttemptWithCallbackUrlsAndVietnamTime() {
        var prepared = service.prepare(customerId, order.getId(), null, "1.2.3.4");

        Payment attempt = payments.get(prepared.paymentId());
        assertThat(attempt.getStatus()).isEqualTo("PENDING");
        assertThat(attempt.getAmount()).isEqualByComparingTo("125000");
        assertThat(attempt.getPaymentCode()).matches("LBP\\d{18}");
        assertThat(prepared.request().amount()).isEqualTo(125_000L);
        assertThat(prepared.request().reference()).isEqualTo(attempt.getPaymentCode());
        assertThat(prepared.request().ipnUrl()).isEqualTo("https://shop.example/api/webhooks/payments/vnpay/ipn");
        assertThat(prepared.request().returnUrl()).isEqualTo("https://shop.example/api/webhooks/payments/vnpay/return");
        assertThat(prepared.request().expiresAt()).isEqualTo(OrderPaymentService.vietnamTime(order.getPaymentDueAt()));
    }

    @Test
    void reusesAStillValidLink() {
        Payment existing = pendingAttempt("LBP1");
        existing.setPayUrl("https://pay/1");
        var prepared = service.prepare(customerId, order.getId(), null, null);
        assertThat(prepared.existing()).isNotNull();
        assertThat(prepared.existing().payUrl()).isEqualTo("https://pay/1");
        assertThat(payments).hasSize(1);
    }

    @Test
    void refusesToPayAnExpiredOrPaidOrder() {
        order.setPaymentDueAt(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.prepare(customerId, order.getId(), null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("quá hạn");
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        assertThatThrownBy(() -> service.prepare(customerId, order.getId(), null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã được thanh toán");
        assertThatThrownBy(() -> service.prepare(UUID.randomUUID(), order.getId(), null, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không tồn tại");
    }

    @Test
    void successfulIpnPaysTheOrderAndShowsItToTheStore() {
        Payment attempt = pendingAttempt("LBP1");
        Payment older = pendingAttempt("LBP0");

        assertThat(service.applyCallback("VNPAY", callback("LBP1", Outcome.SUCCESS, "125000"))).isEqualTo(CallbackResult.CONFIRMED);

        assertThat(attempt.getStatus()).isEqualTo("SUCCESS");
        assertThat(attempt.getTransactionCode()).isEqualTo("14567890");
        assertThat(attempt.getPaidAt()).isNotNull();
        assertThat(older.getStatus()).isEqualTo("CANCELLED");
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getPaymentDueAt()).isNull();
        verify(events).publishEvent(any(OrderEvents.OnlineOrderPlaced.class));
        verify(refunds, never()).save(any());
        assertThat(savedLogs).singleElement().satisfies(log -> {
            assertThat(log.getResult()).isEqualTo("CONFIRMED");
            assertThat(log.getEventKey()).isEqualTo("LBP1:14567890:SUCCESS");
        });
    }

    @Test
    void resentIpnIsNotAppliedTwice() {
        Payment attempt = pendingAttempt("LBP1");
        when(logs.existsByProviderAndEventKey("VNPAY", "LBP1:14567890:SUCCESS")).thenReturn(true);
        assertThat(service.applyCallback("VNPAY", callback("LBP1", Outcome.SUCCESS, "125000")))
                .isEqualTo(CallbackResult.ALREADY_PROCESSED);
        assertThat(attempt.getStatus()).isEqualTo("PENDING");
        verify(events, never()).publishEvent(any());

        attempt.setStatus("SUCCESS");
        when(logs.existsByProviderAndEventKey(anyString(), anyString())).thenReturn(false);
        assertThat(service.applyCallback("VNPAY", callback("LBP1", Outcome.SUCCESS, "125000")))
                .isEqualTo(CallbackResult.ALREADY_PROCESSED);
    }

    @Test
    void amountMismatchAndUnknownReferenceAreRejected() {
        Payment attempt = pendingAttempt("LBP1");
        assertThat(service.applyCallback("VNPAY", callback("LBP1", Outcome.SUCCESS, "1000")))
                .isEqualTo(CallbackResult.AMOUNT_MISMATCH);
        assertThat(attempt.getStatus()).isEqualTo("PENDING");
        assertThat(order.getPaymentStatus()).isEqualTo("AWAITING_PAYMENT");
        assertThat(service.applyCallback("VNPAY", callback("NOPE", Outcome.SUCCESS, "125000")))
                .isEqualTo(CallbackResult.NOT_FOUND);
    }

    @Test
    void failedPaymentKeepsTheOrderWaitingForAnotherTry() {
        Payment attempt = pendingAttempt("LBP1");
        service.applyCallback("VNPAY", new GatewayCallback("LBP1", null, new BigDecimal("125000"), Outcome.FAILED,
                "24/02", "Khách hàng hủy giao dịch", "LBP1:-:24/02", "{}"));
        assertThat(attempt.getStatus()).isEqualTo("FAILED");
        assertThat(attempt.getFailureReason()).isEqualTo("Khách hàng hủy giao dịch");
        assertThat(order.getPaymentStatus()).isEqualTo("AWAITING_PAYMENT");
        verify(events, never()).publishEvent(any());
    }

    @Test
    void moneyArrivingAfterCancellationCreatesAPendingRefund() {
        Payment attempt = pendingAttempt("LBP1");
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(OrderPaymentStatus.ABANDONED);

        service.applyCallback("VNPAY", callback("LBP1", Outcome.SUCCESS, "125000"));

        ArgumentCaptor<Refund> refund = ArgumentCaptor.forClass(Refund.class);
        verify(refunds).save(refund.capture());
        assertThat(refund.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(refund.getValue().getAmount()).isEqualByComparingTo("125000");
        assertThat(refund.getValue().getPaymentId()).isEqualTo(attempt.getId());
        assertThat(refund.getValue().getOrderId()).isEqualTo(order.getId());
        assertThat(attempt.getStatus()).isEqualTo("SUCCESS");
        // Đơn hủy nhưng có tiền: hiện cho cửa hàng/kế toán cùng yêu cầu hoàn tiền.
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void secondSuccessfulAttemptIsRefunded() {
        Payment first = pendingAttempt("LBP1");
        Payment second = pendingAttempt("LBP2");
        service.applyCallback("VNPAY", callback("LBP1", Outcome.SUCCESS, "125000"));
        second.setStatus("PENDING");
        service.applyCallback("VNPAY", callback("LBP2", Outcome.SUCCESS, "125000"));

        ArgumentCaptor<Refund> refund = ArgumentCaptor.forClass(Refund.class);
        verify(refunds).save(refund.capture());
        assertThat(refund.getValue().getPaymentId()).isEqualTo(second.getId());
        assertThat(first.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void overdueOrderIsCancelledBySystemAndRestocked() {
        pendingAttempt("LBP1");
        order.setPaymentDueAt(LocalDateTime.now().minusMinutes(1));

        assertThat(service.expire(order.getId())).isTrue();

        ArgumentCaptor<OrderActor> actor = ArgumentCaptor.forClass(OrderActor.class);
        verify(statusService).transition(eq(order), eq(OrderStatus.CANCELLED), actor.capture(), eq("Quá hạn thanh toán"));
        assertThat(actor.getValue().type()).isEqualTo(OrderActorType.SYSTEM);
        verify(onlineOrders).restock(order, null);
        verify(onlineOrders).markAbandoned(eq(order), eq(List.of(order)), any());
    }

    @Test
    void orderStillWithinDeadlineOrAlreadyPaidIsNotExpired() {
        assertThat(service.expire(order.getId())).isFalse();
        order.setPaymentDueAt(LocalDateTime.now().minusMinutes(1));
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        assertThat(service.expire(order.getId())).isFalse();
        verify(statusService, never()).transition(any(Order.class), any(), any(), any());
    }

    @Test
    void switchingToCodShowsTheOrderToTheStore() {
        Payment attempt = pendingAttempt("LBP1");
        service.switchToCod(customerId, order.getId());

        assertThat(order.getPaymentMethod()).isEqualTo("COD");
        assertThat(order.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(order.getPaymentDueAt()).isNull();
        assertThat(attempt.getStatus()).isEqualTo("CANCELLED");
        verify(events).publishEvent(any(OrderEvents.OnlineOrderPlaced.class));
    }

    @Test
    void lateSuccessAfterSwitchingToCodMarksTheOrderPaidWithoutNotifyingAgain() {
        Payment attempt = pendingAttempt("LBP1");
        order.setPaymentMethod("COD");
        order.setPaymentStatus(OrderPaymentStatus.UNPAID);
        order.setPaymentDueAt(null);

        service.applyCallback("VNPAY", callback("LBP1", Outcome.SUCCESS, "125000"));

        assertThat(attempt.getStatus()).isEqualTo("SUCCESS");
        assertThat(order.getPaymentMethod()).isEqualTo("VNPAY");
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        verify(events, never()).publishEvent(any());
    }

    @Test
    void gatewayAmountsMustBeWholeDong() {
        assertThat(OrderPaymentService.wholeVnd(new BigDecimal("125000.00"))).isEqualTo(125_000L);
        assertThatThrownBy(() -> OrderPaymentService.wholeVnd(new BigDecimal("125000.50")))
                .isInstanceOf(BusinessException.class);
    }
}
