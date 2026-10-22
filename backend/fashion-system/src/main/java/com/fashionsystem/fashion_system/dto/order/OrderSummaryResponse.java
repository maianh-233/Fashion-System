package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Số liệu nhanh cho đầu trang danh sách đơn của một cửa hàng và một loại đơn.
 *
 * @param statusCounts  số đơn đang hiển thị (chưa ẩn) theo trạng thái
 * @param hiddenCount   số đơn đã ẩn
 * @param todayCount    số đơn tạo hôm nay (không tính đơn hủy)
 * @param todayRevenue  tổng tiền đơn hoàn thành tạo hôm nay
 */
public record OrderSummaryResponse(
        Map<OrderStatus, Long> statusCounts, long hiddenCount, long todayCount, BigDecimal todayRevenue) {
}
