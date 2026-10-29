package com.fashionsystem.fashion_system.dto.storefront;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Bảng size theo danh mục, số đo của khách và gợi ý size trên trang sản phẩm. */
public final class SizeAdvisorDtos {
    public static final int MAX_ENTRIES = 20;
    /** Gợi ý lấy từ size khách đã mua và giữ lại trong cùng danh mục. */
    public static final String SOURCE_HISTORY = "HISTORY";
    /** Gợi ý lấy từ bảng size theo chiều cao / cân nặng. */
    public static final String SOURCE_CHART = "CHART";

    private SizeAdvisorDtos() {
    }

    /** Một dòng bảng size; khoảng nào để trống thì không xét. */
    public record ChartEntry(
            @NotBlank(message = "Tên size không được để trống") @Size(max = 50) String size,
            @Min(value = 80, message = "Chiều cao từ 80 đến 250 cm") @Max(value = 250, message = "Chiều cao từ 80 đến 250 cm") Integer minHeightCm,
            @Min(value = 80, message = "Chiều cao từ 80 đến 250 cm") @Max(value = 250, message = "Chiều cao từ 80 đến 250 cm") Integer maxHeightCm,
            @DecimalMin(value = "20", message = "Cân nặng từ 20 đến 250 kg") @DecimalMax(value = "250", message = "Cân nặng từ 20 đến 250 kg") BigDecimal minWeightKg,
            @DecimalMin(value = "20", message = "Cân nặng từ 20 đến 250 kg") @DecimalMax(value = "250", message = "Cân nặng từ 20 đến 250 kg") BigDecimal maxWeightKg) {
    }

    /** Thay toàn bộ bảng size của danh mục, theo thứ tự từ nhỏ đến lớn; danh sách rỗng = xóa bảng. */
    public record ChartRequest(
            @NotNull @Size(max = MAX_ENTRIES, message = "Bảng size tối đa " + MAX_ENTRIES + " dòng") List<@Valid ChartEntry> entries) {
    }

    /** Bảng size riêng của một danh mục (không kế thừa), cho trang quản trị. */
    public record Chart(UUID categoryId, String categoryName, List<ChartEntry> entries) {
    }

    public record Measurements(
            @Min(value = 80, message = "Chiều cao từ 80 đến 250 cm") @Max(value = 250, message = "Chiều cao từ 80 đến 250 cm") Integer heightCm,
            @DecimalMin(value = "20", message = "Cân nặng từ 20 đến 250 kg") @DecimalMax(value = "250", message = "Cân nặng từ 20 đến 250 kg") BigDecimal weightKg) {
    }

    /**
     * Hướng dẫn chọn size cho một sản phẩm.
     *
     * @param chartCategoryName danh mục sở hữu bảng size (có thể là danh mục cha); null khi chưa có bảng
     * @param productSizes      các size đang bán của sản phẩm
     * @param recommendedSize   size nên chọn trong {@code productSizes}; null khi chưa đủ thông tin
     * @param source            {@link #SOURCE_HISTORY} hoặc {@link #SOURCE_CHART}
     * @param chartSize         size theo bảng (kể cả khi sản phẩm không có size đó)
     * @param historySize       size khách hay mua và giữ lại trong cùng danh mục
     */
    public record Guide(String chartCategoryName, List<ChartEntry> entries, List<String> productSizes,
                        Integer heightCm, BigDecimal weightKg, String recommendedSize, String source,
                        String chartSize, String historySize, String note) {
    }
}
