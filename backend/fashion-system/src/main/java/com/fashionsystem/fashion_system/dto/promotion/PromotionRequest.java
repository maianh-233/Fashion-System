package com.fashionsystem.fashion_system.dto.promotion;

import com.fashionsystem.fashion_system.entity.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu tạo hoặc cập nhật khuyến mãi. Mã khuyến mãi và trạng thái do hệ thống quản lý nên không có ở đây.
 */
public record PromotionRequest(
        @NotBlank(message = "Tên khuyến mãi không được để trống") @Size(max = 255) String name,
        @NotNull(message = "Loại khuyến mãi không được để trống") PromotionType promotionType,
        @NotBlank(message = "Kiểu giảm giá không được để trống")
        @Pattern(regexp = "PERCENT|FIXED", message = "Kiểu giảm giá chỉ hỗ trợ PERCENT hoặc FIXED") String discountType,
        @NotNull(message = "Giá trị giảm không được để trống")
        @DecimalMin(value = "0.00", inclusive = false, message = "Giá trị giảm phải lớn hơn 0") BigDecimal discountValue,
        @NotNull(message = "Ngày bắt đầu không được để trống") LocalDateTime startDate,
        @NotNull(message = "Ngày kết thúc không được để trống") LocalDateTime endDate,
        @DecimalMin(value = "0.00", message = "Đơn tối thiểu không được âm") BigDecimal minOrderValue,
        @DecimalMin(value = "0.00", inclusive = false, message = "Giảm tối đa phải lớn hơn 0") BigDecimal maxDiscount,
        @Min(value = 1, message = "Giới hạn lượt dùng phải lớn hơn 0") Integer usageLimit,
        @Min(value = 1, message = "Lượt dùng mỗi khách phải lớn hơn 0") Integer usagePerUser,
        @Min(value = 1, message = "Số điểm cần đổi phải lớn hơn 0") Integer pointsRequired,
        List<UUID> productIds,
        List<UUID> tierIds) {
}
