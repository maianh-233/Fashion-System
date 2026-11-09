package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.config.OrderDeliveryExecutorConfig;
import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.Refund;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.PaymentRepository;
import com.fashionsystem.fashion_system.repository.RefundRepository;
import com.fashionsystem.fashion_system.util.DbTimes;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * Kế toán xử lý các khoản hoàn tiền phát sinh từ thanh toán online: tiền về sau khi đơn đã hủy, khách trả trùng, hủy đơn sau
 * khi giao thất bại. Khoản hoàn của yêu cầu trả hàng được xác nhận ở màn Đổi trả nên không nằm trong danh sách này.
 */
@Slf4j
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("REFUND")
@Transactional(readOnly = true)
public class RefundAdminService {
    static final String MANAGE = "REFUND_MANAGE";
    private static final Set<String> STATUSES = Set.of("PENDING", "COMPLETED", "REJECTED");
    private static final String FROM = """
             from refunds f
             join payments p on p.id = f.payment_id
             left join orders o on o.id = coalesce(f.order_id, p.order_id)
             left join stores s on s.id = o.store_id
             left join customers c on c.id = o.customer_id
             where not exists (select 1 from return_requests r where r.refund_id = f.id)
               and (cast(:store as uuid) is null or o.store_id = :store)
               and (cast(:status as varchar) is null or f.status = :status)
            """;

    public record RefundRow(UUID id, String refundCode, BigDecimal amount, String status, String reason, String rejectReason,
                            boolean duplicatePayment, LocalDateTime requestedAt, LocalDateTime processedAt, UUID orderId,
                            String orderCode, String orderStatus, String storeName, String customerName, String customerPhone,
                            String paymentCode, String paymentMethod, String gatewayTransactionCode) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final StoreAccessService storeAccess;
    private final RefundRepository refunds;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final RefundService refundService;
    private final NotificationPublisher notifications;
    private final TaskExecutor executor;

    public RefundAdminService(NamedParameterJdbcTemplate jdbc, StoreAccessService storeAccess, RefundRepository refunds,
                              PaymentRepository payments, OrderRepository orders, RefundService refundService,
                              NotificationPublisher notifications,
                              @Qualifier(OrderDeliveryExecutorConfig.EXECUTOR) TaskExecutor executor) {
        this.jdbc = jdbc;
        this.storeAccess = storeAccess;
        this.refunds = refunds;
        this.payments = payments;
        this.orders = orders;
        this.refundService = refundService;
        this.notifications = notifications;
        this.executor = executor;
    }

    public Page<RefundRow> list(UUID actor, String status, Pageable pageable) {
        UUID store = storeAccess.requireOrAll(actor, null, MANAGE);
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
        if (normalized != null && !STATUSES.contains(normalized)) throw BusinessException.badRequest("Trạng thái hoàn tiền không hợp lệ");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("store", store, Types.OTHER)
                .addValue("status", normalized, Types.VARCHAR);
        Long total = jdbc.queryForObject("select count(*)" + FROM, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<RefundRow> rows = jdbc.query("""
                select f.id, f.refund_code, f.amount, f.status, f.reason, f.reject_reason, f.duplicate_payment, f.requested_at,
                       f.processed_at, coalesce(o.parent_order_id, o.id) as top_order_id, o.order_code, o.status as order_status,
                       s.name as store_name, c.full_name as customer_name, c.phone as customer_phone,
                       p.payment_code, p.method, p.transaction_code
                """ + FROM + " order by (f.status = 'PENDING') desc, f.requested_at desc limit :limit offset :offset",
                params, (rs, i) -> new RefundRow(rs.getObject("id", UUID.class), rs.getString("refund_code"), rs.getBigDecimal("amount"),
                        rs.getString("status"), rs.getString("reason"), rs.getString("reject_reason"), rs.getBoolean("duplicate_payment"),
                        DbTimes.fromDb(rs.getObject("requested_at")), DbTimes.fromDb(rs.getObject("processed_at")),
                        rs.getObject("top_order_id", UUID.class), rs.getString("order_code"), rs.getString("order_status"),
                        rs.getString("store_name"), rs.getString("customer_name"), rs.getString("customer_phone"),
                        rs.getString("payment_code"), rs.getString("method"), rs.getString("transaction_code")));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    /** Kế toán đã chuyển tiền cho khách; mã giao dịch là bằng chứng đối soát. */
    @Transactional
    public void complete(UUID actor, UUID id, String transactionCode) {
        Order order = requireScope(actor, id);
        refundService.complete(id, transactionCode.trim(), null);
        notifyCustomer(id, order);
    }

    @Transactional
    public void reject(UUID actor, UUID id, String reason) {
        requireScope(actor, id);
        refundService.reject(id, reason);
    }

    private Order requireScope(UUID actor, UUID id) {
        Refund refund = refunds.findById(id).orElseThrow(() -> BusinessException.notFound("Yêu cầu hoàn tiền không tồn tại"));
        UUID orderId = refund.getOrderId() != null ? refund.getOrderId()
                : payments.findById(refund.getPaymentId()).map(p -> p.getOrderId()).orElse(null);
        Order order = orderId == null ? null : orders.findById(orderId).orElse(null);
        if (!storeAccess.allowsScope(actor, order == null ? null : order.getStoreId(), MANAGE))
            throw BusinessException.forbidden("Bạn không có quyền xử lý khoản hoàn tiền này");
        return order;
    }

    private void notifyCustomer(UUID refundId, Order order) {
        if (order == null || order.getCustomerId() == null) return;
        BigDecimal amount = refunds.findById(refundId).map(Refund::getAmount).orElse(BigDecimal.ZERO);
        UUID topOrder = order.getParentOrderId() != null ? order.getParentOrderId() : order.getId();
        NotificationRequest request = new NotificationRequest(List.of(RealtimeIdentity.customer(order.getCustomerId())), null,
                NotificationType.REFUND_COMPLETED, "Đã hoàn tiền đơn " + order.getOrderCode(),
                "Lunaria đã hoàn " + String.format(Locale.GERMANY, "%,d", amount.longValue()) + "đ về tài khoản thanh toán của bạn.",
                "REFUND", refundId, "/orderdetail?id=" + topOrder, Map.of("orderCode", order.getOrderCode()));
        Runnable send = () -> executor.execute(() -> {
            try {
                notifications.publish(request);
            } catch (RuntimeException failure) {
                log.warn("Refund notification for {} failed: {}", refundId, failure.getMessage());
            }
        });
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                send.run();
            }
        });
    }
}
