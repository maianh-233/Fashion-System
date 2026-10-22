package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Order;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm tra phạm vi cửa hàng và quyền cho mọi thao tác quản trị đơn hàng.
 *
 * <p>Nhân viên Store luôn bị giới hạn ở cửa hàng được gán (storeId gửi lên khác cửa hàng của họ
 * trả về 403); nhân viên Global phải chọn một cửa hàng. Quyền xét theo scope của role như
 * các module kho.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderAccessService {
    private final StoreAccessService storeAccess;
    private final UserScopeService userScopes;
    private final OrderRepository orderRepository;

    /** Cửa hàng hiệu lực cho danh sách/tạo đơn; Store tự gán cửa hàng của mình. */
    public UUID resolveStore(UUID actor, UUID requestedStoreId, String permission) {
        return storeAccess.require(actor, requestedStoreId, permission);
    }

    public List<StoreAccessService.StoreOption> accessibleStores(UUID actor) {
        return storeAccess.accessibleStores(actor, List.of(OrderPermissions.VIEW), "Bạn không có quyền xem đơn hàng");
    }

    public boolean isGlobal(UUID actor) {
        return userScopes.resolve(actor).isGlobal();
    }

    /** Tải đơn (không khóa) và kiểm tra nhân viên được thao tác trên cửa hàng của đơn. */
    public Order requireOrder(UUID actor, UUID orderId, String permission) {
        Order order = orderRepository.findById(orderId)
                .filter(OrderAccessService::visibleToStaff)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        requireStore(actor, order, permission);
        return order;
    }

    /**
     * Khóa đơn (PESSIMISTIC_WRITE) và kiểm tra quyền; chỉ gọi trong transaction ghi của nghiệp vụ,
     * nơi thay đổi thực sự được audit.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @com.fashionsystem.fashion_system.audit.AuditInfrastructure(
            reason = "Only acquires a row lock; the calling order operation is audited")
    public Order requireOrderForUpdate(UUID actor, UUID orderId, String permission) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .filter(OrderAccessService::visibleToStaff)
                .orElseThrow(() -> BusinessException.notFound("Đơn hàng không tồn tại"));
        requireStore(actor, order, permission);
        return order;
    }

    /** Đơn trả qua cổng chưa thanh toán (đang chờ hoặc đã bỏ dở) chưa thuộc về cửa hàng nào xử lý. */
    static boolean visibleToStaff(Order order) {
        return !OrderPaymentStatus.hiddenFromStore(order.getPaymentStatus());
    }

    public void requireStore(UUID actor, Order order, String permission) {
        // Đơn tổng của đơn bị tách không thuộc cửa hàng nào; nhân viên xử lý qua từng đơn con.
        if (order.getStoreId() == null)
            throw BusinessException.forbidden("Đơn tổng được xử lý qua các đơn con của từng cửa hàng");
        storeAccess.require(actor, order.getStoreId(), permission);
    }

    public boolean can(UUID actor, Order order, String permission) {
        return order.getStoreId() != null && storeAccess.allows(actor, order.getStoreId(), permission);
    }

    /**
     * Chỉ nhân viên đang xử lý đơn được xác nhận đã nhận hàng hoàn, trừ khi có quyền
     * {@link OrderPermissions#RETURN_OVERRIDE} tại cửa hàng của đơn.
     */
    public void requireReturnConfirmer(UUID actor, Order order) {
        requireStore(actor, order, OrderPermissions.VIEW);
        if (actor.equals(order.getHandlerUserId())) return;
        if (!can(actor, order, OrderPermissions.RETURN_OVERRIDE))
            throw BusinessException.forbidden("Chỉ nhân viên đang xử lý đơn mới được xác nhận nhận hàng hoàn");
    }
}
