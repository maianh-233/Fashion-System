package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.PaymentDto;
import com.fashionsystem.fashion_system.dto.PaymentTransactionDto;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.PaymentTransaction;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PaymentMapper;
import com.fashionsystem.fashion_system.mapper.PaymentTransactionMapper;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PaymentTransactionRepository;
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

/** Quản lý payment và sổ giao dịch payment append-only. */
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final Set<String> PAYMENT_SORT_FIELDS = Set.of(
            "id", "orderId", "paymentCode", "method", "amount", "status", "paidAt", "createdAt", "updatedAt");
    private static final Set<String> TRANSACTION_SORT_FIELDS = Set.of(
            "id", "paymentId", "gatewayTransactionId", "transactionType", "amount", "status", "createdAt");
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentTransactionMapper transactionMapper;

    /** Khởi tạo payment chờ xử lý cho đơn hàng. */
    @Transactional
    public PaymentDto create(PaymentDto request) {
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        if (Set.of("CANCELLED", "RETURNED").contains(order.getStatus()))
            throw BusinessException.invalidState("Không thể thanh toán đơn hàng đã kết thúc");
        if (request.getAmount() == null || request.getAmount().signum() <= 0)
            throw BusinessException.badRequest("Số tiền thanh toán phải lớn hơn 0");
        BigDecimal remaining = order.getTotalAmount().subtract(paymentRepository.sumSuccessfulAmount(order.getId()));
        if (request.getAmount().compareTo(remaining) > 0)
            throw BusinessException.badRequest("Số tiền thanh toán vượt số tiền còn lại của đơn hàng");
        String code = normalizeNullable(request.getPaymentCode());
        if (code != null && paymentRepository.existsByPaymentCode(code))
            throw BusinessException.conflict("Mã thanh toán đã tồn tại");
        Payment payment = Payment.builder().orderId(order.getId()).paymentCode(code)
                .method(normalize(request.getMethod())).amount(request.getAmount()).status("PENDING")
                .createdAt(LocalDateTime.now()).build();
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    /** Lấy chi tiết payment theo ID. */
    @Transactional(readOnly = true)
    public PaymentDto getById(UUID id) { return paymentMapper.toDto(requirePayment(id)); }

    /** Lấy payment với bộ lọc và phân trang. */
    @Transactional(readOnly = true)
    public Page<PaymentDto> getList(
            UUID orderId, String method, String status, String keyword, Pageable pageable) {
        validateSort(pageable, PAYMENT_SORT_FIELDS, "payment");
        return paymentRepository.search(orderId, normalize(method), normalize(status), trimToEmpty(keyword), pageable)
                .map(paymentMapper::toDto);
    }

    /** Ghi nhận payment thành công và đồng bộ payment status của đơn hàng. */
    @Transactional
    public PaymentDto markSuccessful(UUID id, String gatewayTransactionId, String rawResponse) {
        Payment payment = paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Payment không tồn tại"));
        if ("SUCCESS".equals(payment.getStatus())) return paymentMapper.toDto(payment);
        requirePending(payment);
        payment.setStatus("SUCCESS");
        payment.setTransactionCode(trimToNull(gatewayTransactionId));
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        paymentRepository.flush();
        appendTransaction(payment, gatewayTransactionId, "PAYMENT", "SUCCESS", rawResponse);
        syncOrderPaymentStatus(payment.getOrderId(), "PAID");
        return paymentMapper.toDto(payment);
    }

    /** Ghi nhận payment thất bại và lưu phản hồi gateway. */
    @Transactional
    public PaymentDto markFailed(UUID id, String gatewayTransactionId, String rawResponse) {
        Payment payment = paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> BusinessException.notFound("Payment không tồn tại"));
        requirePending(payment);
        payment.setStatus("FAILED");
        payment.setTransactionCode(trimToNull(gatewayTransactionId));
        payment.setUpdatedAt(LocalDateTime.now());
        appendTransaction(payment, gatewayTransactionId, "PAYMENT", "FAILED", rawResponse);
        Order order = orderRepository.findByIdForUpdate(payment.getOrderId())
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        if (paymentRepository.sumSuccessfulAmount(order.getId()).signum() == 0) order.setPaymentStatus("FAILED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    /** Lấy sổ giao dịch append-only của payment theo phân trang. */
    @Transactional(readOnly = true)
    public Page<PaymentTransactionDto> getTransactions(
            UUID paymentId, String type, String status, Pageable pageable) {
        if (paymentId != null) requirePayment(paymentId);
        validateSort(pageable, TRANSACTION_SORT_FIELDS, "giao dịch payment");
        return transactionRepository.search(paymentId, normalize(type), normalize(status), pageable)
                .map(transactionMapper::toDto);
    }

    private void syncOrderPaymentStatus(UUID orderId, String fullyPaidStatus) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        BigDecimal paid = paymentRepository.sumSuccessfulAmount(orderId);
        order.setPaymentStatus(paid.compareTo(order.getTotalAmount()) >= 0 ? fullyPaidStatus : "UNPAID");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
    private void appendTransaction(Payment payment, String gatewayId, String type, String status, String raw) {
        transactionRepository.save(PaymentTransaction.builder().paymentId(payment.getId())
                .gatewayTransactionId(trimToNull(gatewayId)).transactionType(type).amount(payment.getAmount())
                .status(status).rawResponse(raw).createdAt(LocalDateTime.now()).build());
    }
    private Payment requirePayment(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Payment không tồn tại"));
    }
    private void requirePending(Payment payment) {
        if (!"PENDING".equals(payment.getStatus())) throw BusinessException.invalidState("Payment không còn chờ xử lý");
    }
    private void validateSort(Pageable pageable, Set<String> fields, String subject) {
        if (pageable.getSort().stream().anyMatch(o -> !fields.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp " + subject + " không hợp lệ");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String normalizeNullable(String value) {
        String normalized = trimToNull(value); return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String trimToEmpty(String value) { return value == null ? "" : value.trim(); }
}
