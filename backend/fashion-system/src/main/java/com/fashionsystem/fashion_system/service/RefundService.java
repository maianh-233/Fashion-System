package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.RefundDto;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.PaymentTransaction;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.RefundMapper;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PaymentTransactionRepository;
import com.fashionsystem.fashion_system.repository.RefundRepository;
import com.fashionsystem.fashion_system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý yêu cầu hoàn tiền và đồng bộ trạng thái payment/order. */
@Service
@RequiredArgsConstructor
public class RefundService {
    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "paymentId", "refundCode", "amount", "status", "requestedBy", "requestedAt", "processedAt");
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RefundMapper mapper;

    /** Tạo yêu cầu hoàn tiền sau khi kiểm tra số tiền còn có thể hoàn. */
    @Transactional
    public RefundDto request(RefundDto request) {
        Payment payment = paymentRepository.findByIdForUpdate(request.getPaymentId())
                .orElseThrow(() -> BusinessException.notFound("Payment không tồn tại"));
        if (!Set.of("SUCCESS", "REFUNDED").contains(payment.getStatus()))
            throw BusinessException.invalidState("Chỉ payment thành công mới được hoàn tiền");
        if (request.getAmount() == null || request.getAmount().signum() <= 0)
            throw BusinessException.badRequest("Số tiền hoàn phải lớn hơn 0");
        BigDecimal remaining = payment.getAmount().subtract(refundRepository.sumCommittedAmount(payment.getId()));
        if (request.getAmount().compareTo(remaining) > 0)
            throw BusinessException.badRequest("Số tiền hoàn vượt số tiền còn có thể hoàn");
        if (request.getRequestedBy() != null && !userRepository.existsById(request.getRequestedBy()))
            throw BusinessException.notFound("Người yêu cầu hoàn tiền không tồn tại");
        String code = normalizeNullable(request.getRefundCode());
        if (code != null && refundRepository.existsByRefundCode(code))
            throw BusinessException.conflict("Mã hoàn tiền đã tồn tại");
        Refund refund = Refund.builder().paymentId(payment.getId()).refundCode(code).amount(request.getAmount())
                .reason(request.getReason()).status("PENDING").requestedBy(request.getRequestedBy())
                .requestedAt(LocalDateTime.now()).build();
        return mapper.toDto(refundRepository.save(refund));
    }

    /** Lấy chi tiết yêu cầu hoàn tiền. */
    @Transactional(readOnly = true)
    public RefundDto getById(UUID id) { return mapper.toDto(requireRefund(id)); }

    /** Lấy yêu cầu hoàn tiền với bộ lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<RefundDto> getList(UUID paymentId, String status, String keyword, Pageable pageable) {
        validateSort(pageable);
        return refundRepository.search(paymentId, normalize(status), trimToEmpty(keyword), pageable).map(mapper::toDto);
    }

    /** Hoàn tất yêu cầu hoàn tiền và ghi transaction REFUND append-only. */
    @Transactional
    public RefundDto complete(UUID id, String gatewayTransactionId, String rawResponse) {
        Refund refund = requirePendingForUpdate(id);
        Payment payment = paymentRepository.findByIdForUpdate(refund.getPaymentId())
                .orElseThrow(() -> BusinessException.notFound("Payment không tồn tại"));
        refund.setStatus("COMPLETED");
        refund.setProcessedAt(LocalDateTime.now());
        refundRepository.save(refund);
        refundRepository.flush();
        transactionRepository.save(PaymentTransaction.builder().paymentId(payment.getId())
                .gatewayTransactionId(trimToNull(gatewayTransactionId)).transactionType("REFUND")
                .amount(refund.getAmount()).status("SUCCESS").rawResponse(rawResponse)
                .createdAt(LocalDateTime.now()).build());
        BigDecimal refunded = refundRepository.sumCompletedAmount(payment.getId());
        if (refunded.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus("REFUNDED");
            syncOrderStatus(payment.getOrderId(), "REFUNDED");
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return mapper.toDto(refund);
    }

    /** Từ chối yêu cầu hoàn tiền đang chờ xử lý. */
    @Transactional
    public RefundDto reject(UUID id) {
        Refund refund = requirePendingForUpdate(id);
        refund.setStatus("REJECTED");
        refund.setProcessedAt(LocalDateTime.now());
        return mapper.toDto(refundRepository.save(refund));
    }

    private Refund requireRefund(UUID id) {
        return refundRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Yêu cầu hoàn tiền không tồn tại"));
    }
    private Refund requirePendingForUpdate(UUID id) {
        Refund refund = refundRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Yêu cầu hoàn tiền không tồn tại"));
        if (!"PENDING".equals(refund.getStatus())) throw BusinessException.invalidState("Yêu cầu hoàn tiền không còn chờ xử lý");
        return refund;
    }
    private void syncOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        order.setPaymentStatus(status); order.setUpdatedAt(LocalDateTime.now()); orderRepository.save(order);
    }
    private void validateSort(Pageable pageable) {
        if (pageable.getSort().stream().anyMatch(o -> !SORT_FIELDS.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp hoàn tiền không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String normalizeNullable(String value) { String v = trimToNull(value); return v == null ? null : v.toUpperCase(Locale.ROOT); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
