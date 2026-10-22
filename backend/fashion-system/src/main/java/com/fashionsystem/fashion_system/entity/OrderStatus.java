package com.fashionsystem.fashion_system.entity;

import java.util.Set;

/** Trạng thái đơn hàng; lưu dạng chuỗi, nhãn tiếng Việt dùng cho thông báo và giao diện. */
public enum OrderStatus {
    /** Khách đặt online, chờ nhân viên xác nhận. */
    PENDING("Chờ xác nhận"),
    /** Nhân viên đã xác nhận, đang chuẩn bị hàng. */
    CONFIRMED("Đã xác nhận"),
    /** Đã tạo chuyến và bàn giao cho bên vận chuyển. */
    HANDED_TO_SHIPPER("Đã giao cho vận chuyển"),
    /** Bên vận chuyển báo đang giao. */
    SHIPPING("Đang giao"),
    /** Giao thành công hoặc đơn offline đã thu tiền. */
    COMPLETED("Hoàn thành"),
    /** Bên vận chuyển báo giao thất bại, chờ hàng hoàn về cửa hàng. */
    DELIVERY_FAILED("Giao thất bại"),
    /** Nhân viên xử lý đã nhận lại hàng hoàn. */
    RETURNED("Đã hoàn hàng"),
    /** Đơn đã hủy. */
    CANCELLED("Đã hủy");

    private static final Set<OrderStatus> TERMINAL = Set.of(COMPLETED, RETURNED, CANCELLED);

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Trạng thái kết thúc: không còn thao tác nghiệp vụ nào, được phép ẩn đơn. */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}
