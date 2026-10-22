package com.fashionsystem.fashion_system.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Giỏ hàng bán tại quầy, dùng chung cho tính thử (quote), lấy voucher và tạo đơn.
 * Frontend chỉ gửi biến thể và số lượng; mọi giá tiền do backend tính lại.
 *
 * @param storeId        cửa hàng (nhân viên Global chọn; nhân viên Store bỏ trống hoặc gửi đúng cửa hàng mình)
 * @param customerId     khách thành viên; null là khách vãng lai
 * @param promotionCodes voucher đã chọn: tối đa 1 voucher toàn đơn và các khuyến mãi sản phẩm
 * @param cashReceived   tiền khách đưa; bắt buộc khi tạo đơn
 */
public record OfflineCartRequest(
        UUID storeId,
        UUID customerId,
        @NotNull @Size(max = 100, message = "Giỏ hàng tối đa 100 dòng") List<@Valid @NotNull Line> lines,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> promotionCodes,
        @DecimalMin(value = "0", message = "Tiền khách đưa không được âm") BigDecimal cashReceived,
        @Size(max = 1000) String note) {

    public record Line(
            @NotNull UUID variantId,
            @NotNull @Min(value = 1, message = "Số lượng phải lớn hơn 0") @Max(9999) Integer quantity) {
    }

    public List<String> promotionCodesOrEmpty() {
        return promotionCodes == null ? List.of() : promotionCodes;
    }
}
