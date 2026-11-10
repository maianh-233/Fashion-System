package com.fashionsystem.fashion_system.marketing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Báo cáo chiến dịch. Chỉ số doanh thu là của đơn CÓ DÙNG ưu đãi của chiến dịch, không phải doanh thu tăng thêm nhờ chiến
 * dịch; con số "tăng thêm" chỉ có ở {@link Experiment} — so sánh với nhóm đối chứng của đợt gửi. Giới hạn dữ liệu được trả
 * kèm trong {@code dataNotes}.
 */
public final class MarketingReportDtos {
    private MarketingReportDtos() {
    }

    /**
     * @param revenue          tổng tiền hàng của đơn có dùng ưu đãi (gồm đơn đang xử lý; bỏ đơn hủy / trả về / bỏ dở thanh toán)
     * @param completedRevenue phần của đơn đã hoàn tất
     * @param netRevenue       đơn đã hoàn tất trừ tiền hoàn của yêu cầu trả hàng đã hoàn tất
     * @param discountCost     tiền giảm của khuyến mãi / voucher liên kết trên các đơn đó
     * @param flashDiscountCost tiền giảm flash sale — ước tính
     * @param roas             doanh thu thuần / tổng chi phí; null khi chưa có chi phí
     * @param buyersAfterNotification người nhận thông báo có đơn dùng ưu đãi sau lúc nhận (tương quan, không phải nhân quả)
     * @param cogs             giá vốn ước tính của hàng trong đơn đã hoàn tất (giá nhập bình quân gia quyền)
     * @param uncostedQuantity số món chưa có giá nhập nên không tính được giá vốn
     * @param grossProfit      doanh thu thuần − giá vốn (doanh thu đã là sau giảm giá nên không trừ giảm giá lần nữa)
     * @param roi              (lợi nhuận gộp − chi phí thực tế) / chi phí thực tế; null khi chưa có chi phí thực tế
     */
    public record Kpi(long orders, BigDecimal revenue, BigDecimal completedRevenue, BigDecimal refunds, BigDecimal netRevenue,
                      long buyers, long firstTimeBuyers, BigDecimal averageOrderValue, long onlineOrders, long offlineOrders,
                      BigDecimal discountCost, BigDecimal flashDiscountCost, BigDecimal expenseCost, BigDecimal totalCost,
                      Integer budgetUsagePercent, BigDecimal roas, long vouchersIssued, long vouchersUsed,
                      long notificationsSent, long notificationsRead, long buyersAfterNotification,
                      BigDecimal plannedBudget, BigDecimal targetRevenue, Integer targetOrders, Integer targetBuyers,
                      BigDecimal cogs, long uncostedQuantity, BigDecimal grossProfit, BigDecimal roi) {}

    public record DayRow(LocalDate date, long orders, BigDecimal revenue) {}

    /** @param discount null với flash sale khi không ước tính được */
    public record OfferRow(String type, String code, String name, long orders, BigDecimal discount) {}

    /**
     * Thí nghiệm của một đợt gửi có nhóm đối chứng: so tỷ lệ mua (đơn bất kỳ, không chỉ đơn dùng ưu đãi) giữa người đã nhận
     * tin và người bị giữ lại, trong khoảng từ lúc chốt danh sách tới khi chiến dịch kết thúc (hoặc hiện tại).
     *
     * @param upliftPoints        chênh lệch tỷ lệ mua, điểm phần trăm
     * @param zScore              kiểm định z hai tỷ lệ
     * @param significant         |z| ≥ 1,96 (mức tin cậy 95%) và mỗi nhóm có ít nhất 30 người
     * @param incrementalOrders   ước tính số đơn tăng thêm = (đơn/người nhóm nhận − đơn/người nhóm đối chứng) × số người nhận
     * @param incrementalRevenue  ước tính doanh thu tăng thêm, cùng cách tính theo doanh thu
     */
    public record Experiment(UUID dispatchId, String code, String channel, int holdoutPercent, long treated, long treatedBuyers,
                             long holdout, long holdoutBuyers, Double treatedRate, Double holdoutRate, Double upliftPoints,
                             Double zScore, boolean significant, BigDecimal incrementalOrders, BigDecimal incrementalRevenue,
                             LocalDateTime from, LocalDateTime until) {}

    public record Performance(UUID campaignId, String code, String name, String status, LocalDateTime startsAt,
                              LocalDateTime endsAt, Kpi kpi, List<DayRow> daily, List<OfferRow> offers,
                              List<Experiment> experiments, List<String> dataNotes, LocalDateTime generatedAt,
                              UUID storeId, String storeName) {}

    public record CompareRow(UUID id, String code, String name, String status, LocalDateTime startsAt, LocalDateTime endsAt,
                             long orders, BigDecimal revenue, BigDecimal netRevenue, BigDecimal totalCost, BigDecimal roas,
                             long buyers) {}

    /** Số đơn dùng ưu đãi của cả hai chiến dịch: cộng các cột lại sẽ lớn hơn tổng thật đúng bằng phần chồng này. */
    public record Overlap(UUID first, UUID second, long sharedOrders) {}

    /**
     * @param distinctOrders  số đơn khác nhau của tất cả chiến dịch được chọn (mỗi đơn tính một lần)
     * @param distinctRevenue tiền hàng của các đơn đó (gồm đơn đang xử lý)
     */
    public record Comparison(List<CompareRow> rows, long distinctOrders, BigDecimal distinctRevenue, List<Overlap> overlaps,
                             List<String> dataNotes) {}

    /**
     * Một mốc trên lịch.
     *
     * @param kind CAMPAIGN (thời gian chiến dịch), OFFER (thời gian hiệu lực ưu đãi), DISPATCH (giờ gửi thông báo, một thời điểm)
     */
    public record CalendarEvent(String kind, UUID campaignId, String campaignCode, String campaignName, String campaignStatus,
                                String label, String detail, LocalDateTime startsAt, LocalDateTime endsAt, String status) {}

    public record Calendar(LocalDate from, LocalDate to, List<CalendarEvent> events, List<String> conflicts) {}
}
