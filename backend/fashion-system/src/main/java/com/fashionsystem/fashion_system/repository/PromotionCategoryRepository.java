package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionCategory;
import com.fashionsystem.fashion_system.entity.PromotionCategoryId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionCategory.
 */
public interface PromotionCategoryRepository extends BaseRepository<PromotionCategory, PromotionCategoryId> {
    boolean existsByPromotionIdAndCategoryId(UUID promotionId, UUID categoryId);
    void deleteByPromotionIdAndCategoryId(UUID promotionId, UUID categoryId);
    Page<PromotionCategory> findAllByPromotionId(UUID promotionId, Pageable pageable);
}
