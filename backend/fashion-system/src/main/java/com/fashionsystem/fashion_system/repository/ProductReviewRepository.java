package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ProductReview;
import java.util.UUID;

public interface ProductReviewRepository extends BaseRepository<ProductReview, UUID> {
    boolean existsByOrderIdAndProductId(UUID orderId, UUID productId);
}
