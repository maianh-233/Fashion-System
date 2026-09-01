package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionTier;
import com.fashionsystem.fashion_system.entity.PromotionTierId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionTier.
 */
public interface PromotionTierRepository extends BaseRepository<PromotionTier, PromotionTierId> {
    boolean existsByPromotionIdAndTierId(UUID promotionId, UUID tierId);
    void deleteByPromotionIdAndTierId(UUID promotionId, UUID tierId);
    Page<PromotionTier> findAllByPromotionId(UUID promotionId, Pageable pageable);
}
