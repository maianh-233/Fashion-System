package com.fashionsystem.fashion_system.marketing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Dữ liệu vào / ra của module chiến dịch marketing. */
public final class MarketingCampaignDtos {
    private MarketingCampaignDtos() {
    }

    public record Ref(UUID id, String name) {}

    /**
     * @param global     nhân viên cấp chuỗi (thấy mọi trạng thái và số liệu tài chính)
     * @param canManage  tạo / sửa / gửi duyệt / hủy / liên kết ưu đãi / nhập chi phí
     * @param canApprove duyệt hoặc từ chối
     * @param owners     nhân viên cấp chuỗi đang hoạt động, dùng để chọn người phụ trách
     * @param tiers      hạng khách hàng, dùng cho tiêu chí nhóm khách của đợt gửi
     * @param categories danh mục đang hoạt động, dùng cho tiêu chí "từng mua danh mục"
     */
    public record Lookups(boolean global, boolean canManage, boolean canApprove, boolean canLinkOffers, List<Ref> owners,
                          List<Ref> tiers, List<Ref> categories, UUID storeId, String storeName, List<Ref> stores) {}

    /**
     * Nhóm khách của một đợt gửi: khách đang hoạt động thỏa tất cả tiêu chí được chọn; không chọn gì là mọi khách.
     * Cùng ý nghĩa với tiêu chí của chiến dịch voucher.
     */
    public record AudienceCriteria(@Size(max = 20) List<@NotNull UUID> tierIds,
                                   @Min(value = 1, message = "Số ngày chưa mua phải từ 1")
                                   @Max(value = 1095, message = "Số ngày chưa mua tối đa 1095") Integer inactiveDays,
                                   UUID categoryId) {
        public List<UUID> tierIdsOrEmpty() {
            return tierIds == null ? List.of() : tierIds.stream().distinct().toList();
        }
    }

    public record DispatchRequest(@NotNull @jakarta.validation.Valid AudienceCriteria criteria,
                                  @NotBlank @Size(max = 120) String title,
                                  @NotBlank @Size(max = 500) String message,
                                  @Size(max = 500) String actionUrl,
                                  String channel,
                                  @NotNull LocalDateTime scheduledAt,
                                  @Min(0) @Max(50) Integer holdoutPercent) {}

    /**
     * Xem trước nhóm khách cho một kênh; {@code campaignId} để giới hạn theo cửa hàng của chiến dịch (bắt buộc với nhân viên
     * cửa hàng).
     */
    public record PreviewRequest(@NotNull @jakarta.validation.Valid AudienceCriteria criteria, String channel, UUID campaignId) {}

    /** Tiêu chí đã lưu kèm nhãn lúc tạo (hiển thị được cả khi hạng / danh mục đổi tên hoặc bị xóa). */
    public record Audience(List<UUID> tierIds, Integer inactiveDays, UUID categoryId, List<String> tierNames,
                           String categoryName) {}

    public record DispatchActions(boolean edit, boolean approve, boolean reject, boolean cancel, boolean retry) {}

    /**
     * Một đợt gửi. Các số đếm lấy trực tiếp từ danh sách người nhận đã chốt.
     *
     * @param outsideWindow giờ gửi nằm ngoài khoảng cho phép (từ 7 ngày trước khi bắt đầu tới lúc kết thúc chiến dịch)
     */
    public record Dispatch(UUID id, String code, String channel, String title, String message, String actionUrl,
                           Audience audience, LocalDateTime scheduledAt, String status, Integer audienceCount,
                           int pending, int sent, int skipped, int failed, int read, LocalDateTime snapshotAt,
                           LocalDateTime startedAt, LocalDateTime finishedAt, String createdByName, String approvedByName,
                           LocalDateTime approvedAt, String cancelReason, LocalDateTime cancelledAt, boolean outsideWindow,
                           DispatchActions actions, int holdoutPercent, int holdout) {}

    /** Một người nhận trong danh sách đã chốt; số điện thoại đã che bớt. */
    public record Recipient(UUID customerId, String customerCode, String fullName, String phone, String status,
                            String skipReason, int attemptCount, LocalDateTime sentAt, LocalDateTime readAt,
                            String lastErrorCode, String lastError) {}

    public record PreviewCustomer(String customerCode, String fullName, String phone, boolean hasAccount) {}

    /**
     * Ước lượng nhóm khách lúc xem trước; danh sách thật được chốt lúc bắt đầu gửi.
     *
     * @param matched           khách đang hoạt động khớp tiêu chí
     * @param reachable         trong đó nhận được qua kênh này: in-app = có tài khoản online và không tắt tin marketing;
     *                          email = có email và đã đồng ý nhận email khuyến mãi
     * @param afterFrequencyCap trong đó chưa nhận thông báo marketing trong khoảng giới hạn tần suất
     */
    public record Preview(long matched, long reachable, long afterFrequencyCap, int frequencyCapHours,
                          List<PreviewCustomer> sample, String channel) {}

    public record CampaignRequest(@NotBlank @Size(max = 150) String name,
                                  @NotBlank String objective,
                                  @Size(max = 5000) String description,
                                  @NotNull LocalDateTime startsAt,
                                  @NotNull LocalDateTime endsAt,
                                  UUID ownerId,
                                  @DecimalMin("0") @DecimalMax("999999999999") BigDecimal plannedBudget,
                                  @DecimalMin("0") @DecimalMax("999999999999") BigDecimal targetRevenue,
                                  @Min(0) @Max(100_000_000) Integer targetOrders,
                                  @Min(0) @Max(100_000_000) Integer targetBuyers,
                                  UUID storeId) {}

    public record ReasonRequest(@NotBlank @Size(min = 5, max = 500) String reason) {}

    public record NoteRequest(@Size(max = 500) String note) {}

    public record LinkRequest(@NotBlank String type, @NotNull UUID itemId) {}

    public record ExpenseRequest(@NotBlank String category,
                                 @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("999999999999") BigDecimal amount,
                                 @NotNull LocalDate spentOn,
                                 @Size(max = 150) String vendor,
                                 @Size(max = 500) String note) {}

    /**
     * Một chiến dịch. Với nhân viên cửa hàng các trường tài chính (ngân sách, chỉ tiêu, tổng chi phí) là null.
     *
     * @param phase UPCOMING / RUNNING / ENDED khi APPROVED, null với trạng thái khác (tính từ thời gian, không lưu)
     */
    public record Campaign(UUID id, String code, String name, String objective, String description,
                           LocalDateTime startsAt, LocalDateTime endsAt, UUID ownerId, String ownerName,
                           String status, String phase, BigDecimal plannedBudget, BigDecimal targetRevenue,
                           Integer targetOrders, Integer targetBuyers, int offerCount, BigDecimal expenseTotal,
                           UUID createdBy, String createdByName, LocalDateTime submittedAt, String submittedByName,
                           LocalDateTime decidedAt, String decidedByName, String decisionNote,
                           LocalDateTime cancelledAt, String cancelledByName, String cancelReason,
                           LocalDateTime createdAt, LocalDateTime updatedAt, UUID storeId, String storeName) {}

    /**
     * Ưu đãi đã liên kết hoặc ứng viên để liên kết.
     *
     * @param inEffect       còn hiệu lực hoặc sắp hiệu lực (chưa hết hạn, chưa bị tắt / hủy)
     * @param outsideWindow  thời gian hiệu lực của ưu đãi nằm ngoài thời gian chiến dịch (chỉ cảnh báo)
     */
    public record Offer(UUID id, String type, UUID itemId, String code, String name, String detail,
                        LocalDateTime validFrom, LocalDateTime validTo, String itemStatus, boolean inEffect,
                        boolean outsideWindow, String linkedByName, LocalDateTime linkedAt) {}

    /**
     * @param linkedCampaignCode chiến dịch đang giữ ưu đãi (null = còn trống)
     * @param reason             lý do không chọn được (null = chọn được)
     */
    public record Candidate(String type, UUID itemId, String code, String name, String detail, LocalDateTime validFrom,
                            LocalDateTime validTo, String itemStatus, UUID linkedCampaignId, String linkedCampaignCode,
                            String linkedCampaignName, String reason) {}

    public record Expense(UUID id, String category, BigDecimal amount, LocalDate spentOn, String vendor, String note,
                          String createdByName, LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record History(UUID id, String action, String fromStatus, String toStatus, String note, String actorName,
                          LocalDateTime createdAt) {}

    /** Thao tác tài khoản hiện tại được làm trên chiến dịch; giao diện dựa vào đây để hiện nút. */
    public record Actions(boolean edit, boolean submit, boolean withdraw, boolean approve, boolean reject, boolean reopen,
                          boolean cancel, boolean linkOffers, boolean unlinkOffers, boolean manageExpenses,
                          boolean viewFinancials, boolean viewHistory, boolean addDispatch) {}

    /**
     * @param warnings cảnh báo không chặn: ưu đãi nằm ngoài thời gian chiến dịch, vượt ngân sách, ưu đãi còn hiệu lực sau khi hủy
     */
    public record Detail(Campaign campaign, List<Offer> offers, List<Dispatch> dispatches, List<Expense> expenses,
                         List<History> history, Actions actions, List<String> warnings) {}
}
