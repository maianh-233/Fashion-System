package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.service.StoreAccessService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Thông tin khởi tạo trang quản trị đơn hàng.
 *
 * @param global   true khi nhân viên cấp chuỗi (hiện ô chọn cửa hàng)
 * @param storeId  cửa hàng cố định của nhân viên Store; null với Global
 * @param stores   cửa hàng được chọn
 * @param statuses mã và nhãn tiếng Việt của trạng thái đơn
 */
public record OrderAdminMetaResponse(
        boolean global,
        UUID storeId,
        List<StoreAccessService.StoreOption> stores,
        List<StatusOption> statuses) {

    public record StatusOption(OrderStatus code, String label, boolean terminal) {
        public static List<StatusOption> all() {
            return Arrays.stream(OrderStatus.values())
                    .map(status -> new StatusOption(status, status.label(), status.isTerminal())).toList();
        }
    }
}
