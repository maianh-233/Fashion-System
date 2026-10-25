package com.fashionsystem.fashion_system.dto.dashboard;

import com.fashionsystem.fashion_system.entity.OrderStatus;
import com.fashionsystem.fashion_system.entity.OrderType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Số liệu trang tổng quan quản trị cho một cửa hàng hoặc toàn hệ thống ({@code storeId} null).
 * Danh sách nào nhân viên thiếu quyền xem (đơn, tồn kho, phiếu nhập, phiếu xuất) trả về null để giao diện ẩn đi.
 *
 * @param month          chỉ số từ đầu tháng đến hôm nay, so với cùng kỳ tháng trước
 * @param dailyRevenue   doanh thu 7 ngày gần nhất (gồm hôm nay)
 * @param yearlyRevenue  doanh thu 5 năm gần nhất (năm nay tính đến hôm nay)
 * @param bestSellers    sản phẩm bán chạy nhất trong tháng
 */
public record DashboardResponse(
        LocalDate today, UUID storeId, String storeName, MonthStats month,
        List<RevenuePoint> dailyRevenue, List<RevenuePoint> yearlyRevenue, List<BestSeller> bestSellers,
        List<RecentOrder> recentOrders, List<LowStockItem> lowStock,
        List<RecentReceipt> recentImports, List<RecentIssue> recentExports) {

    /**
     * Doanh thu và số sản phẩm bán chỉ tính đơn hoàn thành; số đơn không tính đơn hủy.
     * Khách mới theo cửa hàng là khách có cửa hàng nguồn đó (khách tự đăng ký web chỉ hiện ở toàn hệ thống).
     */
    public record MonthStats(
            BigDecimal revenue, BigDecimal previousRevenue, long orders, long previousOrders,
            long newCustomers, long previousNewCustomers, long itemsSold, long previousItemsSold) {
    }

    /** {@code label} dạng {@code yyyy-MM-dd} cho ngày hoặc {@code yyyy} cho năm. */
    public record RevenuePoint(String label, BigDecimal revenue) {
    }

    public record BestSeller(UUID productId, String productName, String imageUrl, Long quantity, BigDecimal revenue) {
    }

    public record RecentOrder(UUID id, String orderCode, OrderType orderType, OrderStatus status, BigDecimal totalAmount,
                              LocalDateTime createdAt, String customerName, String storeName) {
    }

    /** {@code quantity} là tổng tồn tại quầy và tồn bán online của biến thể ở cửa hàng. */
    public record LowStockItem(UUID productVariantId, String productName, String sku, String color, String size,
                               UUID storeId, String storeName, Integer quantity) {
    }

    public record RecentReceipt(UUID id, String code, String supplierName, String storeName, Integer totalQuantity,
                                BigDecimal totalAmount, String status, LocalDateTime createdAt) {
    }

    public record RecentIssue(UUID id, String code, String issueType, String storeName, Integer totalQuantity,
                              String status, LocalDateTime createdAt) {
    }
}
