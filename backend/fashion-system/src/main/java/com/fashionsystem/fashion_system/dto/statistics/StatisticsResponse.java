package com.fashionsystem.fashion_system.dto.statistics;

import com.fashionsystem.fashion_system.dto.dashboard.DashboardResponse;
import com.fashionsystem.fashion_system.entity.OrderType;
import com.fashionsystem.fashion_system.service.StatisticsPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Báo cáo thống kê cho một kỳ, một cửa hàng (hoặc toàn hệ thống khi {@code storeId} null) và một loại đơn
 * ({@code orderType} null là mọi loại). Ngày {@code to} và {@code previousTo} là mốc loại trừ (nửa mở).
 * Tồn kho và khách mới không phụ thuộc loại đơn.
 */
public record StatisticsResponse(
        StatisticsPeriod period, StatisticsPeriod.Granularity granularity,
        LocalDate from, LocalDate to, LocalDate previousFrom, LocalDate previousTo,
        UUID storeId, String storeName, OrderType orderType,
        Overview overview, List<SeriesPoint> revenueSeries, Map<String, Long> statusCounts,
        List<Breakdown> byOrderType, List<Breakdown> byPaymentMethod,
        List<TopProduct> topProducts, List<DashboardResponse.LowStockItem> lowStock,
        List<MonthCount> newCustomersByMonth, Retention retention, List<VipCustomer> vipCustomers,
        Inventory inventory) {

    /**
     * Doanh thu tính đơn hoàn thành; số đơn không tính đơn hủy. Lợi nhuận gộp ước tính = doanh thu trừ phí vận
     * chuyển trừ giá vốn theo giá nhập bình quân; {@code uncostedQuantity} là số sản phẩm đã bán chưa có giá nhập
     * (tính giá vốn 0, nên lợi nhuận bị cao hơn thực tế).
     */
    public record Overview(BigDecimal revenue, BigDecimal previousRevenue, long orders, long previousOrders,
                           BigDecimal grossProfit, BigDecimal previousGrossProfit, long uncostedQuantity,
                           long newCustomers, long previousNewCustomers) {
    }

    /** {@code label} dạng {@code yyyy-MM-dd} theo ngày hoặc {@code yyyy-MM} theo tháng. */
    public record SeriesPoint(String label, BigDecimal revenue, long orders) {
    }

    /** Phân bổ theo loại đơn hoặc phương thức thanh toán. */
    public record Breakdown(String key, long orders, BigDecimal revenue) {
    }

    /** {@code stock} là tồn hiện tại (quầy + online) của sản phẩm trong phạm vi đang xem. */
    public record TopProduct(UUID productId, String productName, long sold, BigDecimal revenue, long stock) {
    }

    /** {@code month} dạng {@code yyyy-MM}. */
    public record MonthCount(String month, long count) {
    }

    /** Khách có đơn trong kỳ và tỷ lệ khách đã mua từ 2 đơn trở lên (tính đến hết kỳ). */
    public record Retention(long activeCustomers, long returningCustomers, Double rate, Double previousRate) {
    }

    public record VipCustomer(UUID id, String fullName, String customerCode, String tier, long orders,
                              BigDecimal spend) {
    }

    /**
     * Nhập / xuất: phiếu hoàn thành trong kỳ. Tồn và giá trị tồn là hiện tại, giá trị theo giá nhập bình quân;
     * {@code uncostedVariants} là số dòng tồn chưa có giá nhập. {@code unsoldVariants}: dòng tồn còn hàng nhưng
     * không bán được sản phẩm nào trong kỳ tại cửa hàng đó.
     */
    public record Inventory(long importedQuantity, long exportedQuantity, long stockQuantity, BigDecimal stockValue,
                            long uncostedVariants, long unsoldVariants) {
    }
}
