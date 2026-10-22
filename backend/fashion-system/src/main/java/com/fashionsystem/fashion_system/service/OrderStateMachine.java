package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.OrderActorType;
import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

import static com.fashionsystem.fashion_system.entity.OrderActorType.CUSTOMER;
import static com.fashionsystem.fashion_system.entity.OrderActorType.SHIPPER;
import static com.fashionsystem.fashion_system.entity.OrderActorType.SYSTEM;
import static com.fashionsystem.fashion_system.entity.OrderActorType.USER;
import static com.fashionsystem.fashion_system.entity.OrderStatus.*;

/**
 * Bảng chuyển trạng thái đơn hàng duy nhất của hệ thống.
 *
 * <p>Mỗi cặp (trạng thái hiện tại → trạng thái mới) khai báo luôn loại tác nhân được phép
 * thực hiện, ví dụ chỉ webhook của bên vận chuyển mới chuyển được sang SHIPPING. Các quy tắc
 * phụ thuộc dữ liệu đơn (đã thanh toán qua cổng, người xử lý...) nằm ở service nghiệp vụ.</p>
 */
@Component
public class OrderStateMachine {
    private static final Map<OrderStatus, Map<OrderStatus, Set<OrderActorType>>> RULES = new EnumMap<>(OrderStatus.class);

    static {
        allow(PENDING, CONFIRMED, USER);
        // Khách chỉ được hủy khi nhân viên chưa xác nhận; hệ thống hủy đơn trả qua cổng quá hạn thanh toán.
        allow(PENDING, CANCELLED, USER, CUSTOMER, SYSTEM);
        // Hãng hủy chuyến rồi tạo chuyến thay thế (cancel-and-clone): đơn đã về "Đã xác nhận" được giao lại tự động.
        allow(CONFIRMED, HANDED_TO_SHIPPER, USER, SHIPPER);
        allow(CONFIRMED, CANCELLED, USER);
        allow(HANDED_TO_SHIPPER, SHIPPING, SHIPPER);
        // Hãng không tìm được tài xế / từ chối / hủy chuyến: đơn quay lại để nhân viên xử lý tiếp.
        allow(HANDED_TO_SHIPPER, CONFIRMED, SHIPPER);
        // Webhook có thể đến không đúng thứ tự (bỏ lỡ sự kiện lấy hàng) nhưng giao thành công vẫn phải ghi nhận.
        allow(HANDED_TO_SHIPPER, COMPLETED, SHIPPER);
        allow(SHIPPING, COMPLETED, SHIPPER);
        allow(SHIPPING, DELIVERY_FAILED, SHIPPER);
        // Nhân viên xử lý đơn xác nhận đã nhận lại hàng hoàn.
        allow(DELIVERY_FAILED, RETURNED, USER);
    }

    private static void allow(OrderStatus from, OrderStatus to, OrderActorType... actors) {
        RULES.computeIfAbsent(from, ignored -> new EnumMap<>(OrderStatus.class)).put(to, Set.of(actors));
    }

    /** Trạng thái khởi tạo: đơn online chờ xác nhận, đơn offline thu tiền mặt nên hoàn thành ngay. */
    public OrderStatus initialStatus(OrderType type) {
        return type == OrderType.OFFLINE ? COMPLETED : PENDING;
    }

    public boolean canTransition(OrderStatus from, OrderStatus to, OrderActorType actor) {
        return allowedActors(from, to).contains(actor);
    }

    /** Các trạng thái mà tác nhân có thể chuyển tới từ trạng thái hiện tại (dùng để bật/tắt nút thao tác). */
    public Set<OrderStatus> nextStatuses(OrderStatus from, OrderActorType actor) {
        Set<OrderStatus> result = EnumSet.noneOf(OrderStatus.class);
        RULES.getOrDefault(from, Map.of()).forEach((to, actors) -> {
            if (actors.contains(actor)) result.add(to);
        });
        return result;
    }

    public void requireTransition(OrderStatus from, OrderStatus to, OrderActorType actor) {
        if (from == null || to == null)
            throw BusinessException.badRequest("Trạng thái đơn hàng không hợp lệ");
        Set<OrderActorType> actors = allowedActors(from, to);
        if (actors.isEmpty())
            throw BusinessException.invalidState("Không thể chuyển đơn hàng từ \"" + from.label()
                    + "\" sang \"" + to.label() + "\"");
        if (!actors.contains(actor))
            throw BusinessException.forbidden("Bạn không được chuyển đơn hàng sang \"" + to.label() + "\"");
    }

    private Set<OrderActorType> allowedActors(OrderStatus from, OrderStatus to) {
        return RULES.getOrDefault(from, Map.of()).getOrDefault(to, Set.of());
    }
}
