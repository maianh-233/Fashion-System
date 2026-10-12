package com.fashionsystem.fashion_system.entity;

/** Vòng đời của khuyến mãi; lưu dạng chuỗi để có thể bổ sung trạng thái mới. */
public enum PromotionStatus {
    /** Đã tạo nhưng chưa tới ngày bắt đầu. */
    SCHEDULED,
    /** Đang trong thời gian hiệu lực. */
    ACTIVE,
    /** Bị xóa mềm hoặc tắt thủ công. */
    INACTIVE,
    /** Đã quá ngày kết thúc. */
    EXPIRED
}
