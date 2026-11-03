package com.fashionsystem.fashion_system.dto.promotion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Chiến dịch voucher theo nhóm khách: tiêu chí nhóm, mức giảm, lịch gửi và kết quả. */
public final class VoucherCampaignDtos {
    /** Mặc định bỏ qua khách đã nhận voucher chiến dịch trong chừng này ngày. */
    public static final int DEFAULT_SKIP_RECENT_DAYS = 14;

    public static final Map<String, String> STATUS_LABELS = Map.of(
            "SCHEDULED", "Đã hẹn gửi",
            "SENT", "Đã gửi",
            "CANCELLED", "Đã hủy");

    private VoucherCampaignDtos() {
    }

    /**
     * Nhóm khách: khách đang hoạt động thỏa tất cả tiêu chí được chọn; không chọn tiêu chí nào là mọi khách.
     *
     * @param tierIds      hạng hiện tại thuộc một trong các hạng này
     * @param inactiveDays đã từng mua (có đơn hoàn thành) nhưng không có đơn nào trong chừng này ngày
     * @param categoryId   đã có đơn hoàn thành chứa sản phẩm thuộc danh mục này (gồm danh mục con)
     * @param skipRecent   bỏ qua khách đã nhận voucher chiến dịch trong {@value #DEFAULT_SKIP_RECENT_DAYS} ngày
     */
    public record Criteria(
            @Size(max = 20) List<@NotNull UUID> tierIds,
            @Min(value = 1, message = "Số ngày chưa mua phải từ 1") @Max(value = 1095, message = "Số ngày chưa mua tối đa 1095")
            Integer inactiveDays,
            UUID categoryId,
            Boolean skipRecent) {
        public List<UUID> tierIdsOrEmpty() {
            return tierIds == null ? List.of() : tierIds.stream().distinct().toList();
        }

        public boolean skipRecentOrDefault() {
            return skipRecent == null || skipRecent;
        }
    }

    /**
     * @param scheduledAt giờ gửi (giờ Việt Nam); bỏ trống là gửi ngay
     * @param validDays   voucher dùng được tới hết ngày thứ {@code validDays} kể từ ngày gửi
     * @param message     lời nhắn thêm vào thông báo / email (không bắt buộc)
     */
    public record CreateRequest(
            @NotBlank(message = "Vui lòng nhập tên chiến dịch") @Size(max = 150) String name,
            @Valid @NotNull Criteria criteria,
            @NotNull @Pattern(regexp = "PERCENT|FIXED") String discountType,
            @NotNull @DecimalMin(value = "0", inclusive = false, message = "Mức giảm phải lớn hơn 0") BigDecimal discountValue,
            @DecimalMin(value = "0", inclusive = false, message = "Giảm tối đa phải lớn hơn 0") BigDecimal maxDiscount,
            @DecimalMin(value = "0", message = "Đơn tối thiểu không được âm") BigDecimal minOrderValue,
            @NotNull @Min(value = 1, message = "Voucher phải dùng được ít nhất 1 ngày")
            @Max(value = 365, message = "Voucher dùng được tối đa 365 ngày") Integer validDays,
            @Size(max = 500) String message,
            LocalDateTime scheduledAt) {
    }

    public record Preview(long count, List<Recipient> sample) {
    }

    public record Recipient(UUID id, String customerCode, String fullName, String phone, String tierName) {
    }

    /**
     * @param recipientCount số khách đã nhận voucher (null khi chưa gửi)
     * @param usedCount      số voucher đã dùng
     * @param usedRevenue    tổng tiền các đơn (không hủy) đã dùng voucher của chiến dịch
     */
    public record Campaign(
            UUID id, String code, String name, String status, String statusLabel,
            UUID promotionId, String promotionCode, String discountType, BigDecimal discountValue,
            BigDecimal maxDiscount, BigDecimal minOrderValue, int validDays, String message,
            List<String> tierNames, Integer inactiveDays, UUID categoryId, String categoryName, Integer skipRecentDays,
            LocalDateTime scheduledAt, LocalDateTime sentAt, Integer recipientCount, long usedCount, BigDecimal usedRevenue,
            String createdByName, LocalDateTime createdAt, LocalDateTime cancelledAt) {
    }
}
