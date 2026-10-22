package com.fashionsystem.fashion_system.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Giỏ hàng online của khách. Chỉ gửi biến thể + số lượng; giá, phân cửa hàng và tồn kho do backend quyết định.
 *
 * @param addressId       địa chỉ giao trong sổ địa chỉ của khách (phải có tọa độ)
 * @param paymentMethod   hiện hỗ trợ COD; VNPAY / MOMO được nối ở giai đoạn thanh toán
 * @param clientRequestId khóa chống đặt trùng do frontend sinh cho mỗi lần bấm "Đặt hàng"
 */
public record OnlineCheckoutRequest(
        @NotNull UUID addressId,
        @NotEmpty @Size(max = 100) List<OfflineCartRequest.@Valid @NotNull Line> lines,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> promotionCodes,
        @Pattern(regexp = "COD|VNPAY|MOMO") String paymentMethod,
        @Size(max = 1000) String note,
        @Size(max = 64) String clientRequestId) {

    public List<String> promotionCodesOrEmpty() {
        return promotionCodes == null ? List.of() : promotionCodes;
    }
}
