package com.fashionsystem.fashion_system.entity;

/** Trạng thái nội bộ của một chuyến giao hàng (độc lập với mã trạng thái của từng hãng). */
public enum ShipmentStatus {
    /** Đã tạo chuyến, hãng đang tìm tài xế. */
    ASSIGNING_DRIVER,
    /** Tài xế đã nhận chuyến, đang tới lấy hàng. */
    DRIVER_ASSIGNED,
    /** Tài xế đã lấy hàng, đang giao. */
    PICKED_UP,
    /** Giao thành công. */
    COMPLETED,
    /** Hãng không tìm được tài xế / từ chối / hủy chuyến; nhân viên cần xử lý tiếp. */
    FAILED,
    /** Cửa hàng chủ động hủy chuyến. */
    CANCELLED,
    /** Hãng hủy và tạo chuyến thay thế (cancel-and-clone); xem replaced_by_shipment_id. */
    REPLACED;

    /** Chuyến còn đang chạy: chưa thể tạo chuyến mới cho cùng đơn. */
    public boolean isActive() {
        return this == ASSIGNING_DRIVER || this == DRIVER_ASSIGNED || this == PICKED_UP;
    }
}
