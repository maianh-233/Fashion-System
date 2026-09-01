package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionBrand;
import com.fashionsystem.fashion_system.entity.PromotionBrandId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionBrand.
 */
public interface PromotionBrandRepository extends BaseRepository<PromotionBrand, PromotionBrandId> {
    boolean existsByPromotionIdAndBrandId(UUID promotionId, UUID brandId);
    void deleteByPromotionIdAndBrandId(UUID promotionId, UUID brandId);
    Page<PromotionBrand> findAllByPromotionId(UUID promotionId, Pageable pageable);
}
