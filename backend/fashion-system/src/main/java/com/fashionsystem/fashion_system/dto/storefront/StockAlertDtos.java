package com.fashionsystem.fashion_system.dto.storefront;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** "Báo khi có hàng": khách đăng ký biến thể đang hết hàng online, được báo một lần khi có hàng lại. */
public final class StockAlertDtos {
    public static final int MAX_PENDING = 50;

    private StockAlertDtos() {
    }

    /** Một đăng ký; {@code notifiedAt} null là đang chờ. */
    public record Alert(UUID id, UUID productId, UUID variantId, String productName, String color, String size,
                        String imageUrl, boolean inStock, LocalDateTime createdAt, LocalDateTime notifiedAt) {
    }

    /** Biến thể của một sản phẩm mà khách đang chờ (để trang sản phẩm hiện "Đã đăng ký"). */
    public record Pending(List<UUID> variantIds) {
    }

    /** Thông báo cần gửi sau khi đánh dấu đăng ký đã được báo. */
    public record Restocked(UUID alertId, UUID customerId, UUID productId, String productName, String color, String size) {
    }
}
