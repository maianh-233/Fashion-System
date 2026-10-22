package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đơn COD giao thành công: tài xế đã thu tiền nên ghi nhận thanh toán tự động.
 *
 * <p>Phiếu thu nằm ở đơn khách thanh toán (đơn tổng nếu đơn bị tách), mã {@code COD-<mã phần đơn>} là duy nhất nên
 * webhook gửi lại không ghi hai lần. Đơn tổng PAID khi đã thu đủ, giữa chừng là PARTIALLY_PAID.</p>
 */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PAYMENT")
@RequiredArgsConstructor
public class CodCollectionService {
    static final String METHOD = "COD";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderAmountCalculator amounts;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCollected(Order storeOrder, String providerReference) {
        Order owner = storeOrder.getParentOrderId() == null ? storeOrder
                : orderRepository.findByIdForUpdate(storeOrder.getParentOrderId()).orElseThrow();
        if (!METHOD.equals(owner.getPaymentMethod())) return;
        String code = "COD-" + storeOrder.getOrderCode();
        if (paymentRepository.existsByPaymentCode(code)) return;
        LocalDateTime now = LocalDateTime.now();
        BigDecimal due = amounts.amountDue(storeOrder);
        paymentRepository.save(Payment.builder().orderId(owner.getId()).paymentCode(code).method(METHOD).amount(due)
                .status("SUCCESS").transactionCode(providerReference).paidAt(now).createdAt(now).build());
        paymentRepository.flush();
        if (owner != storeOrder) {
            storeOrder.setPaymentStatus("PAID");
            storeOrder.setUpdatedAt(now);
            orderRepository.save(storeOrder);
        }
        BigDecimal paid = paymentRepository.sumSuccessfulAmount(owner.getId());
        owner.setPaymentStatus(paid.compareTo(owner.getTotalAmount()) >= 0 ? "PAID" : "PARTIALLY_PAID");
        owner.setUpdatedAt(now);
        orderRepository.save(owner);
    }
}
