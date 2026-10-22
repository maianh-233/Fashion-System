package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mỗi phút hủy các đơn trả qua cổng đã quá hạn thanh toán (mỗi đơn một transaction). */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {
    private final OrderRepository orderRepository;
    private final OrderPaymentService payments;

    @Scheduled(fixedDelayString = "${integrations.payment.expiry-check-ms:60000}",
            initialDelayString = "${integrations.payment.expiry-check-ms:60000}")
    public void expireOverdue() {
        for (UUID orderId : orderRepository.findOverduePaymentOrderIds(LocalDateTime.now())) {
            try (var ignored = BusinessAuditContextFactory.systemJob("PAYMENT_EXPIRY")) {
                if (payments.expire(orderId)) log.info("Cancelled order {}: payment deadline passed", orderId);
            } catch (RuntimeException failure) {
                log.warn("Could not expire unpaid order {}: {}", orderId, failure.getMessage());
            }
        }
    }
}
