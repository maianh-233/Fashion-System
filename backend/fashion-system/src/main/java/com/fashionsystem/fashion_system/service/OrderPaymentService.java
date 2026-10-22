package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.CustomerOrderResponse;
import com.fashionsystem.fashion_system.dto.order.PaymentStartResponse;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.Payment;
import com.fashionsystem.fashion_system.entity.PaymentTransaction;
import com.fashionsystem.fashion_system.entity.PaymentWebhookLog;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.payment.PaymentGateway;
import com.fashionsystem.fashion_system.payment.PaymentGatewayRegistry;
import com.fashionsystem.fashion_system.payment.PaymentModels.GatewayCallback;
import com.fashionsystem.fashion_system.payment.PaymentModels.Outcome;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentLink;
import com.fashionsystem.fashion_system.payment.PaymentModels.PaymentRequest;
import com.fashionsystem.fashion_system.payment.PaymentProperties;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.PaymentTransactionRepository;
import com.fashionsystem.fashion_system.repository.PaymentWebhookLogRepository;
import com.fashionsystem.fashion_system.repository.RefundRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các bước ghi dữ liệu của thanh toán qua cổng, mỗi bước một transaction. Gọi HTTP tới cổng nằm ở
 * {@link OrderPaymentFacade}, ngoài transaction, để không giữ khóa đơn trong lúc chờ mạng.
 *
 * <p>Quy tắc: đơn trả qua cổng giữ hàng tới {@code payment_due_at} và chưa hiện cho cửa hàng. Thanh toán thành
 * công thì đơn hiện cho cửa hàng (gửi thông báo đơn mới). Quá hạn chưa trả thì hệ thống hủy đơn, cộng lại kho,
 * không hoàn voucher. Khách trả thất bại thì tạo link mới hoặc đổi sang COD. Tiền về khi đơn đã hủy hoặc đã được
 * trả bằng lần khác thì ghi nhận và tạo yêu cầu hoàn tiền PENDING để kế toán xử lý.</p>
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PAYMENT")
@RequiredArgsConstructor
@Transactional
public class OrderPaymentService {
    static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String EXPIRED_REASON = "Quá hạn thanh toán";
    private static final DateTimeFormatter REFERENCE_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final RefundRepository refundRepository;
    private final OnlineOrderService onlineOrders;
    private final OrderStatusService statusService;
    private final PaymentGatewayRegistry gateways;
    private final PaymentProperties properties;
    private final ApplicationEventPublisher events;

    /** Kết quả xử lý callback; controller đổi sang mã phản hồi riêng của từng cổng. */
    public enum CallbackResult {
        CONFIRMED, ALREADY_PROCESSED, NOT_FOUND, AMOUNT_MISMATCH, INVALID_SIGNATURE
    }

    /**
     * Lần thanh toán đã ghi, chờ tạo link. {@code existing} khác null khi link cũ còn hạn được dùng lại.
     */
    public record Prepared(UUID paymentId, String gatewayCode, PaymentRequest request, PaymentStartResponse existing) {
    }

    /** Dữ liệu hiển thị khi khách quay lại từ cổng. */
    public record ReturnView(UUID orderId, String orderCode, String attemptStatus) {
    }

    /**
     * Ghi một lần thanh toán PENDING cho đơn đang chờ thanh toán. Khách có thể chọn cổng khác ({@code method});
     * link cũ cùng cổng còn hạn thì dùng lại để bấm nhiều lần không sinh nhiều giao dịch.
     */
    public Prepared prepare(UUID customerId, UUID orderId, String method, String clientIp) {
        Order top = requireOwnAwaitingOrder(customerId, orderId);
        LocalDateTime now = LocalDateTime.now();
        String code = method == null || method.isBlank() ? top.getPaymentMethod() : method.trim().toUpperCase(Locale.ROOT);
        PaymentGateway gateway = gateways.byCode(code).filter(PaymentGateway::configured)
                .orElseThrow(() -> BusinessException.badRequest("Cổng thanh toán " + code + " chưa sẵn sàng"));
        long amount = wholeVnd(top.getTotalAmount());
        if (!gateway.code().equals(top.getPaymentMethod())) setPaymentMethod(top, gateway.code(), now);

        Optional<Payment> reusable = paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(top.getId()).stream()
                .filter(p -> gateway.code().equals(p.getMethod()) && OrderPaymentStatus.ATTEMPT_PENDING.equals(p.getStatus())
                        && p.getPayUrl() != null && p.getExpiresAt() != null && p.getExpiresAt().isAfter(now.plusMinutes(1)))
                .reduce((first, second) -> second);
        if (reusable.isPresent()) return new Prepared(reusable.get().getId(), gateway.code(), null, toStart(reusable.get()));

        Payment payment = paymentRepository.save(Payment.builder().orderId(top.getId()).paymentCode(newReference(now))
                .method(gateway.code()).amount(top.getTotalAmount()).status(OrderPaymentStatus.ATTEMPT_PENDING)
                .expiresAt(top.getPaymentDueAt()).createdAt(now).updatedAt(now).build());
        String path = "/api/webhooks/payments/" + gateway.code().toLowerCase(Locale.ROOT);
        PaymentRequest request = new PaymentRequest(payment.getPaymentCode(), amount,
                "Thanh toan don hang " + top.getOrderCode(), clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp,
                vietnamTime(now), vietnamTime(top.getPaymentDueAt()), properties.callbackUrl(path + "/return"),
                properties.callbackUrl(path + "/ipn"));
        return new Prepared(payment.getId(), gateway.code(), request, null);
    }

    /** Lưu link cổng trả về cho lần thanh toán. */
    public PaymentStartResponse attachLink(UUID paymentId, PaymentLink link) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow();
        payment.setPayUrl(link.payUrl());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        if (link.rawResponse() != null)
            transactionRepository.save(PaymentTransaction.builder().paymentId(payment.getId()).transactionType("CREATE")
                    .amount(payment.getAmount()).status(OrderPaymentStatus.ATTEMPT_PENDING).rawResponse(link.rawResponse())
                    .createdAt(LocalDateTime.now()).build());
        return toStart(payment);
    }

    /** Cổng không tạo được link: lần thanh toán này thất bại, khách thử lại hoặc đổi sang COD. */
    public void markLinkFailed(UUID paymentId, String reason) {
        paymentRepository.findByIdForUpdate(paymentId).ifPresent(payment -> {
            if (!OrderPaymentStatus.ATTEMPT_PENDING.equals(payment.getStatus())) return;
            payment.setStatus(OrderPaymentStatus.ATTEMPT_FAILED);
            payment.setFailureReason(truncate("Không tạo được link: " + reason));
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        });
    }

    /** Khách đổi sang thanh toán khi nhận hàng: đơn hiện cho cửa hàng như đơn COD bình thường. */
    public CustomerOrderResponse switchToCod(UUID customerId, UUID orderId) {
        Order top = requireOwnAwaitingOrder(customerId, orderId);
        LocalDateTime now = LocalDateTime.now();
        for (Order order : withStoreOrders(top)) {
            order.setPaymentMethod(OnlineOrderService.DEFAULT_PAYMENT);
            order.setPaymentStatus(OrderPaymentStatus.UNPAID);
            order.setPaymentDueAt(null);
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }
        cancelPendingAttempts(top.getId(), null, "Khách đổi sang thanh toán khi nhận hàng", now);
        events.publishEvent(onlineOrders.placedEvent(top));
        return onlineOrders.myOrder(customerId, orderId);
    }

    /**
     * Áp kết quả IPN đã xác minh chữ ký. Khóa đơn trước rồi tới lần thanh toán (cùng thứ tự với tạo link và hủy
     * quá hạn) nên IPN gửi lại song song chỉ được áp một lần.
     */
    public CallbackResult applyCallback(String gatewayCode, GatewayCallback callback) {
        LocalDateTime now = LocalDateTime.now();
        Payment found = paymentRepository.findByPaymentCode(callback.reference())
                .filter(p -> gatewayCode.equals(p.getMethod())).orElse(null);
        if (found == null) return log(gatewayCode, callback, null, null, CallbackResult.NOT_FOUND, now);
        Order top = orderRepository.findByIdForUpdate(found.getOrderId()).orElseThrow();
        Payment payment = paymentRepository.findByIdForUpdate(found.getId()).orElseThrow();

        if (webhookLogRepository.existsByProviderAndEventKey(gatewayCode, callback.eventKey())
                || OrderPaymentStatus.ATTEMPT_SUCCESS.equals(payment.getStatus()))
            return log(gatewayCode, callback, payment.getId(), null, CallbackResult.ALREADY_PROCESSED, now);
        if (callback.amount().compareTo(payment.getAmount()) != 0) {
            log.warn("{} IPN for {} reports {} but payment is {}", gatewayCode, payment.getPaymentCode(),
                    callback.amount(), payment.getAmount());
            return log(gatewayCode, callback, payment.getId(), null, CallbackResult.AMOUNT_MISMATCH, now);
        }

        transactionRepository.save(PaymentTransaction.builder().paymentId(payment.getId())
                .gatewayTransactionId(callback.gatewayTransactionId()).transactionType("PAYMENT")
                .amount(callback.amount()).status(callback.outcome().name()).rawResponse(callback.rawJson())
                .createdAt(now).build());
        switch (callback.outcome()) {
            case PENDING -> {
                // Cổng chưa có kết quả cuối; chờ IPN tiếp theo.
            }
            case FAILED -> {
                if (OrderPaymentStatus.ATTEMPT_PENDING.equals(payment.getStatus())) {
                    payment.setStatus(OrderPaymentStatus.ATTEMPT_FAILED);
                    payment.setTransactionCode(callback.gatewayTransactionId());
                    payment.setFailureReason(truncate(callback.message()));
                    payment.setUpdatedAt(now);
                    paymentRepository.save(payment);
                }
            }
            case SUCCESS -> paid(top, payment, callback, now);
        }
        return log(gatewayCode, callback, payment.getId(), callback.eventKey(), CallbackResult.CONFIRMED, now);
    }

    /** Callback sai chữ ký / không đọc được: chỉ lưu vết, không đụng tới đơn. */
    public void logRejected(String gatewayCode, String payload, String reason) {
        LocalDateTime now = LocalDateTime.now();
        webhookLogRepository.save(PaymentWebhookLog.builder().provider(gatewayCode).eventType("REJECTED")
                .payload(payload == null || payload.isBlank() ? "{}" : payload).processed(false).signatureValid(false)
                .result(truncate(CallbackResult.INVALID_SIGNATURE.name() + ": " + reason, 50)).createdAt(now).build());
    }

    /**
     * Hủy đơn quá hạn thanh toán (tác nhân hệ thống): cộng lại kho, voucher không hoàn, khách được báo hủy.
     *
     * @return false nếu đơn đã được trả / đổi COD / hủy trong lúc chờ
     */
    public boolean expire(UUID orderId) {
        Order top = orderRepository.findByIdForUpdate(orderId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (top == null || !OrderPaymentStatus.AWAITING_PAYMENT.equals(top.getPaymentStatus())
                || top.getPaymentDueAt() == null || top.getPaymentDueAt().isAfter(now)) return false;
        List<Order> locked = new ArrayList<>();
        for (Order storeOrder : onlineOrders.storeOrders(top)) {
            Order order = storeOrder.getId().equals(top.getId()) ? top
                    : orderRepository.findByIdForUpdate(storeOrder.getId()).orElseThrow();
            if (order.getStatus() == OrderStatus.PENDING) {
                statusService.transition(order, OrderStatus.CANCELLED, OrderActor.system(), EXPIRED_REASON);
                onlineOrders.restock(order, null);
            }
            locked.add(order);
        }
        onlineOrders.markAbandoned(top, locked, now);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<ReturnView> returnView(String reference) {
        return paymentRepository.findByPaymentCode(reference).flatMap(payment -> orderRepository.findById(payment.getOrderId())
                .map(order -> new ReturnView(order.getId(), order.getOrderCode(), payment.getStatus())));
    }

    private void paid(Order top, Payment payment, GatewayCallback callback, LocalDateTime now) {
        boolean paidByOtherAttempt = paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(top.getId()).stream()
                .anyMatch(p -> !p.getId().equals(payment.getId()) && OrderPaymentStatus.ATTEMPT_SUCCESS.equals(p.getStatus()));
        payment.setStatus(OrderPaymentStatus.ATTEMPT_SUCCESS);
        payment.setTransactionCode(callback.gatewayTransactionId());
        payment.setFailureReason(null);
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);
        paymentRepository.save(payment);

        boolean cancelled = top.getStatus() == OrderStatus.CANCELLED;
        if (cancelled || paidByOtherAttempt) {
            String reason = cancelled
                    ? "Khách thanh toán sau khi đơn " + top.getOrderCode() + " đã hủy"
                    : "Đơn " + top.getOrderCode() + " đã được thanh toán bằng giao dịch khác";
            refundRepository.save(Refund.builder().paymentId(payment.getId()).orderId(top.getId()).amount(payment.getAmount())
                    .status("PENDING").requestedAt(now).reason(reason + " (mã " + payment.getPaymentCode() + ")").build());
            // Đơn hủy nhưng đã có tiền: hiện cho cửa hàng/kế toán thấy cùng yêu cầu hoàn tiền.
            if (cancelled) withStoreOrders(top).forEach(order -> {
                order.setPaymentMethod(payment.getMethod());
                order.setPaymentStatus(OrderPaymentStatus.PAID);
                order.setUpdatedAt(now);
                orderRepository.save(order);
            });
            log.warn("Refund requested for {} payment {}: {}", payment.getMethod(), payment.getPaymentCode(), reason);
            return;
        }

        boolean wasHidden = OrderPaymentStatus.AWAITING_PAYMENT.equals(top.getPaymentStatus());
        for (Order order : withStoreOrders(top)) {
            order.setPaymentMethod(payment.getMethod());
            order.setPaymentStatus(OrderPaymentStatus.PAID);
            order.setPaymentDueAt(null);
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }
        cancelPendingAttempts(top.getId(), payment.getId(), "Đơn đã được thanh toán", now);
        // Đơn chờ thanh toán chưa từng báo cửa hàng; đơn đã đổi sang COD thì cửa hàng đã được báo.
        if (wasHidden) events.publishEvent(onlineOrders.placedEvent(top));
    }

    private Order requireOwnAwaitingOrder(UUID customerId, UUID orderId) {
        Order top = orderRepository.findByIdForUpdate(orderId)
                .filter(o -> customerId.equals(o.getCustomerId()) && o.getParentOrderId() == null)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        if (OrderPaymentStatus.PAID.equals(top.getPaymentStatus()))
            throw BusinessException.invalidState("Đơn đã được thanh toán");
        if (!OrderPaymentStatus.AWAITING_PAYMENT.equals(top.getPaymentStatus()) || top.getStatus() != OrderStatus.PENDING)
            throw BusinessException.invalidState("Đơn không còn chờ thanh toán");
        if (top.getPaymentDueAt() == null || !top.getPaymentDueAt().isAfter(LocalDateTime.now()))
            throw BusinessException.invalidState("Đơn đã quá hạn thanh toán");
        return top;
    }

    /** Đơn của khách cùng các đơn con (đã khóa); đơn thường chỉ gồm chính nó. */
    private List<Order> withStoreOrders(Order top) {
        List<Order> all = new ArrayList<>();
        all.add(top);
        if (top.getStoreId() == null)
            orderRepository.findAllByParentOrderId(top.getId()).stream().sorted(Comparator.comparing(Order::getId))
                    .forEach(child -> all.add(orderRepository.findByIdForUpdate(child.getId()).orElseThrow()));
        return all;
    }

    private void setPaymentMethod(Order top, String method, LocalDateTime now) {
        for (Order order : withStoreOrders(top)) {
            order.setPaymentMethod(method);
            order.setUpdatedAt(now);
            orderRepository.save(order);
        }
    }

    private void cancelPendingAttempts(UUID orderId, UUID keep, String reason, LocalDateTime now) {
        paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .filter(p -> !p.getId().equals(keep) && OrderPaymentStatus.ATTEMPT_PENDING.equals(p.getStatus()))
                .forEach(p -> {
                    p.setStatus(OrderPaymentStatus.ATTEMPT_CANCELLED);
                    p.setFailureReason(reason);
                    p.setUpdatedAt(now);
                    paymentRepository.save(p);
                });
    }

    private CallbackResult log(String gatewayCode, GatewayCallback callback, UUID paymentId, String eventKey,
                               CallbackResult result, LocalDateTime now) {
        webhookLogRepository.save(PaymentWebhookLog.builder().provider(gatewayCode).eventType(callback.outcome().name())
                .payload(callback.rawJson()).processed(result == CallbackResult.CONFIRMED).processedAt(now)
                .eventKey(eventKey).paymentId(paymentId).signatureValid(true).result(result.name()).createdAt(now).build());
        return result;
    }

    private static PaymentStartResponse toStart(Payment payment) {
        return new PaymentStartResponse(payment.getId(), payment.getMethod(), payment.getAmount(), payment.getPayUrl(),
                payment.getExpiresAt());
    }

    /** Cổng nhận số tiền VND nguyên; tổng tiền đơn luôn làm tròn tới đồng nên phần lẻ là lỗi dữ liệu. */
    static long wholeVnd(BigDecimal amount) {
        try {
            return amount.stripTrailingZeros().longValueExact();
        } catch (ArithmeticException fractional) {
            throw BusinessException.invalidState("Số tiền thanh toán không hợp lệ");
        }
    }

    /** Giờ của ứng dụng (múi giờ JVM) đổi sang giờ Việt Nam mà cổng yêu cầu. */
    static LocalDateTime vietnamTime(LocalDateTime appTime) {
        return appTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(VIETNAM).toLocalDateTime();
    }

    /** Mã tham chiếu gửi cổng: chỉ chữ và số (yêu cầu của vnp_TxnRef), không lặp. */
    static String newReference(LocalDateTime now) {
        return "LBP" + REFERENCE_TIME.format(now) + String.format("%03d", RANDOM.nextInt(1000));
    }

    private static String truncate(String value) {
        return truncate(value, 255);
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
