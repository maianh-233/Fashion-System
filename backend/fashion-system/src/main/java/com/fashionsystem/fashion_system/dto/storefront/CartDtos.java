package com.fashionsystem.fashion_system.dto.storefront;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Giỏ hàng online. Giá và tồn kho tính lại từ catalog mỗi lần đọc (không lưu trong giỏ). */
public final class CartDtos {
    private CartDtos() {
    }

    public static final int MAX_QUANTITY = 99;
    public static final int MAX_LINES = 50;

    /** Biến thể + thông tin hiển thị/giá/tồn online hiện tại. */
    public record VariantInfo(UUID variantId, UUID productId, String slug, String productName, String brandName,
                              String sku, String color, String size, String imageUrl, BigDecimal price,
                              BigDecimal salePrice, int available, boolean sellable) {
        public BigDecimal unitPrice() {
            return salePrice != null ? salePrice : price;
        }
    }

    /**
     * Một dòng giỏ.
     *
     * @param problem   null nếu mua được; ngược lại lý do (ngừng bán, hết hàng, chỉ còn N)
     * @param lineTotal đơn giá × số lượng (chưa tính voucher; voucher tính ở bước thanh toán)
     */
    public record Line(UUID variantId, UUID productId, String slug, String productName, String brandName, String sku,
                       String color, String size, String imageUrl, BigDecimal price, BigDecimal salePrice,
                       BigDecimal unitPrice, int quantity, boolean selected, int available, boolean sellable,
                       BigDecimal lineTotal, String problem) {
    }

    /**
     * @param itemCount        tổng số lượng sản phẩm trong giỏ (cho huy hiệu giỏ hàng)
     * @param selectedSubtotal tạm tính các dòng được chọn và mua được
     * @param canCheckout      có ít nhất một dòng được chọn và mọi dòng được chọn đều mua được
     */
    public record Cart(List<Line> items, int itemCount, int selectedCount, BigDecimal selectedSubtotal,
                       boolean canCheckout) {
    }

    public record AddItem(@NotNull UUID variantId, @NotNull @Min(1) @Max(MAX_QUANTITY) Integer quantity) {
    }

    /** Đổi số lượng và/hoặc trạng thái chọn; trường null giữ nguyên. */
    public record UpdateItem(@Min(1) @Max(MAX_QUANTITY) Integer quantity, Boolean selected) {
    }

    public record SelectAll(@NotNull Boolean selected) {
    }

    /** Giỏ trên trình duyệt khi chưa đăng nhập (xem trước hoặc gộp vào giỏ tài khoản). */
    public record GuestCart(@NotNull @Size(max = MAX_LINES) List<@Valid @NotNull AddItem> items) {
    }
}
