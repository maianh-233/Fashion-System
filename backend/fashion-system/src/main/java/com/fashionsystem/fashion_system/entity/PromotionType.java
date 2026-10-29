package com.fashionsystem.fashion_system.entity;

/** Loại khuyến mãi; quyết định các trường cấu hình được phép sử dụng. */
public enum PromotionType {
    /** Giảm trên giá trị đơn hàng. */
    ORDER,
    /** Giảm cho các sản phẩm được chọn. */
    PRODUCT,
    /** Khách hàng dùng điểm tích lũy để đổi lấy khuyến mãi. */
    POINT_REDEMPTION,
    /** Mẫu voucher sinh nhật: hệ thống phát cho từng khách vào đầu tháng sinh, dùng trong tháng đó. */
    BIRTHDAY
}
