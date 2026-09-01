package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionProduct;
import com.fashionsystem.fashion_system.entity.PromotionProductId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionProduct.
 */
public interface PromotionProductRepository extends BaseRepository<PromotionProduct, PromotionProductId> {
    boolean existsByPromotionIdAndProductId(UUID promotionId, UUID productId);
    void deleteByPromotionIdAndProductId(UUID promotionId, UUID productId);
    Page<PromotionProduct> findAllByPromotionId(UUID promotionId, Pageable pageable);
}
