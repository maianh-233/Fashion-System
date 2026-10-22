package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderStatusHistory;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import com.fashionsystem.fashion_system.repository.OrderStatusHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nơi duy nhất thay đổi {@code orders.status}: kiểm tra bảng chuyển trạng thái, cập nhật đơn
 * và ghi một dòng lịch sử trong cùng transaction với nghiệp vụ gọi tới.
 */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("ORDER")
@RequiredArgsConstructor
public class OrderStatusService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderStateMachine stateMachine;
    private final ApplicationEventPublisher events;

    /** Gán trạng thái khởi tạo theo loại đơn cho đơn chưa lưu. */
    public void initialize(Order order) {
        order.setStatus(stateMachine.initialStatus(order.getOrderType()));
    }

    /** Ghi dòng lịch sử đầu tiên sau khi đơn mới đã được lưu. */
    @Transactional
    public void recordCreated(Order order, OrderActor actor, String note) {
        if (order.getId() == null) throw new IllegalStateException("Order must be saved before recording history");
        appendHistory(order.getId(), null, order.getStatus(), actor, null, note);
    }

    /** Khóa đơn rồi chuyển trạng thái. */
    @Transactional
    public Order transition(UUID orderId, OrderStatus target, OrderActor actor, String reason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        return transition(order, target, actor, reason);
    }

    /**
     * Chuyển trạng thái cho đơn đã được khóa ({@code findByIdForUpdate}) bởi nghiệp vụ gọi tới.
     *
     * @param reason lý do; bắt buộc khi nhân viên hủy đơn vì được gửi cho khách
     */
    @Transactional
    public Order transition(Order order, OrderStatus target, OrderActor actor, String reason) {
        OrderStatus from = order.getStatus();
        stateMachine.requireTransition(from, target, actor.type());
        String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
        if (target == OrderStatus.CANCELLED) {
            if (actor.type() == OrderActorType.USER && normalizedReason == null)
                throw BusinessException.badRequest("Vui lòng nhập lý do hủy đơn");
            order.setCancelReason(normalizedReason);
            order.setCancelledByType(actor.type());
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(target);
        order.setUpdatedAt(now);
        Order saved = orderRepository.save(order);
        appendHistory(order.getId(), from, target, actor, normalizedReason, null);
        events.publishEvent(new OrderEvents.StatusChanged(saved.getId(), saved.getOrderCode(), saved.getOrderType(),
                saved.getStoreId(), saved.getCustomerId(), saved.getParentOrderId(), saved.getHandlerUserId(), from,
                target, actor, normalizedReason));
        if (saved.getParentOrderId() != null) syncParent(saved.getParentOrderId());
        return saved;
    }

    /**
     * Đơn tổng của đơn bị tách không có quy trình riêng; trạng thái của nó chỉ phản ánh các đơn con khi
     * tất cả đã kết thúc: tất cả bị hủy → CANCELLED, có ít nhất một phần hoàn thành → COMPLETED, còn lại
     * (chỉ gồm hủy và hoàn hàng) → RETURNED. Trong lúc các đơn con đang xử lý, đơn tổng giữ PENDING và
     * khách xem trạng thái từng phần.
     */
    @Transactional
    public void syncParent(UUID parentId) {
        Order parent = orderRepository.findByIdForUpdate(parentId)
                .orElseThrow(() -> BusinessException.notFound("Đơn tổng không tồn tại"));
        List<OrderStatus> children = orderRepository.findAllByParentOrderId(parentId).stream()
                .map(Order::getStatus).toList();
        if (children.isEmpty() || !children.stream().allMatch(OrderStatus::isTerminal)) return;
        OrderStatus target = children.stream().allMatch(s -> s == OrderStatus.CANCELLED) ? OrderStatus.CANCELLED
                : children.contains(OrderStatus.COMPLETED) ? OrderStatus.COMPLETED : OrderStatus.RETURNED;
        if (parent.getStatus() == target) return;
        OrderStatus from = parent.getStatus();
        parent.setStatus(target);
        parent.setUpdatedAt(LocalDateTime.now());
        if (target == OrderStatus.CANCELLED) parent.setCancelledByType(OrderActorType.SYSTEM);
        orderRepository.save(parent);
        appendHistory(parentId, from, target, OrderActor.system(), null, "Cập nhật theo trạng thái các phần của đơn");
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistory> history(UUID orderId) {
        return historyRepository.findAllByOrderIdOrderByChangedAtAsc(orderId);
    }

    private void appendHistory(UUID orderId, OrderStatus from, OrderStatus to, OrderActor actor,
                               String reason, String note) {
        historyRepository.save(OrderStatusHistory.builder()
                .orderId(orderId).fromStatus(from).toStatus(to)
                .actorType(actor.type()).changedBy(actor.id())
                .reason(reason).note(note).changedAt(LocalDateTime.now()).build());
    }
}
