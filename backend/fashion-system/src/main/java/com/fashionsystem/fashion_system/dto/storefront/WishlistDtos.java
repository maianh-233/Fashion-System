package com.fashionsystem.fashion_system.dto.storefront;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Danh sách yêu thích của khách: chỉ lưu sản phẩm; thẻ sản phẩm lấy qua API sản phẩm công khai. */
public final class WishlistDtos {
    public static final int MAX_ITEMS = 200;

    private WishlistDtos() {
    }

    /** Sản phẩm yêu thích còn đang bán, mới thêm trước. */
    public record Wishlist(List<UUID> productIds) {
    }

    /** Danh sách lưu trên trình duyệt khi chưa đăng nhập, gộp vào tài khoản khi đăng nhập. */
    public record GuestWishlist(@NotNull @Size(max = MAX_ITEMS) List<@NotNull UUID> productIds) {
    }
}
