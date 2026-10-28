package com.fashionsystem.fashion_system.dto.storefront;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Đánh giá sản phẩm: chỉ khách có đơn hoàn thành chứa sản phẩm mới đánh giá được, mỗi (đơn, sản phẩm) một lần. */
public final class ReviewDtos {
    public static final int MAX_CONTENT = 2000;
    public static final int MAX_IMAGES = 3;
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private ReviewDtos() {
    }

    /** Điểm trung bình và số đánh giá đang hiển thị; {@code stars[0]} là số đánh giá 5 sao, ... {@code stars[4]} là 1 sao. */
    public record Summary(BigDecimal average, long count, List<Long> stars, long withImages) {
    }

    /** Đánh giá hiển thị công khai trên trang sản phẩm; tên khách đã được che bớt. */
    public record PublicReview(UUID id, int rating, String content, String reviewerName, String variantLabel,
                               List<String> images, boolean verifiedPurchase, String reply, LocalDateTime repliedAt,
                               LocalDateTime createdAt) {
    }

    /** Sản phẩm khách đã nhận (đơn hoàn thành) nhưng chưa đánh giá. */
    public record Pending(UUID orderId, String orderCode, UUID productId, UUID variantId, String productName,
                          String variantLabel, String imageUrl, LocalDateTime completedAt) {
    }

    /** Đánh giá của chính khách, kể cả khi đã bị cửa hàng ẩn. */
    public record MyReview(UUID id, UUID orderId, String orderCode, UUID productId, String productName,
                           String variantLabel, String productImage, int rating, String content, List<String> images,
                           boolean hidden, String reply, LocalDateTime createdAt) {
    }

    /** Một dòng trong trang quản trị đánh giá. */
    public record AdminReview(UUID id, UUID productId, String productName, String productCode, UUID orderId,
                              String orderCode, UUID customerId, String customerName, String variantLabel, int rating,
                              String content, List<String> images, boolean hidden, String hiddenReason,
                              LocalDateTime hiddenAt, String reply, LocalDateTime repliedAt, LocalDateTime createdAt) {
    }

    public record VisibilityRequest(@NotNull Boolean hidden, @Size(max = 500) String reason) {
    }

    /** Để trống để gỡ phản hồi. */
    public record ReplyRequest(@Size(max = MAX_CONTENT) String reply) {
    }
}
