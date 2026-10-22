package com.fashionsystem.fashion_system.dto.order;

import jakarta.validation.constraints.Size;

/**
 * Khách hủy đơn: chọn một lý do có sẵn (hoặc bỏ trống) và ghi chú tùy chọn.
 *
 * @param reasonCode mã trong {@link CancelReason}, có thể null
 */
public record CustomerCancelRequest(@Size(max = 40) String reasonCode, @Size(max = 500) String note) {

    /** Lý do hủy khách được chọn. */
    public enum CancelReason {
        CHANGE_MIND("Đổi ý, không muốn mua nữa"),
        WRONG_ITEM("Đặt nhầm sản phẩm, màu hoặc size"),
        CHANGE_ADDRESS("Muốn đổi địa chỉ giao hàng"),
        FOUND_BETTER_PRICE("Tìm được nơi bán giá tốt hơn"),
        DELIVERY_TOO_LONG("Thời gian giao hàng quá lâu"),
        OTHER("Lý do khác");

        private final String label;

        CancelReason(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record ReasonOption(String code, String label) {
    }
}
