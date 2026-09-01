package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.PaymentDto;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.mapper.PaymentMapper;
import com.fashionsystem.fashion_system.mapper.PaymentTransactionMapper;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    @Test
    void successfulPaymentAppendsTransactionAndMarksFullyPaidOrder() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        PaymentMapper mapper = mock(PaymentMapper.class);
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder().id(paymentId).orderId(orderId).status("PENDING")
                .amount(new BigDecimal("100.00")).build();
        Order order = Order.builder().id(orderId).totalAmount(new BigDecimal("100.00"))
                .paymentStatus("UNPAID").build();
        PaymentDto response = PaymentDto.builder().id(paymentId).status("SUCCESS").build();
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.sumSuccessfulAmount(orderId)).thenReturn(new BigDecimal("100.00"));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(mapper.toDto(payment)).thenReturn(response);
        PaymentService service = new PaymentService(paymentRepository, transactionRepository,
                orderRepository, mapper, mock(PaymentTransactionMapper.class));

        PaymentDto result = service.markSuccessful(paymentId, "GW-1", "{}");

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(order.getPaymentStatus()).isEqualTo("PAID");
        verify(transactionRepository).save(any());
        verify(orderRepository).save(order);
    }
}
