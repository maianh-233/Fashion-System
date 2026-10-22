package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionTier;
import com.fashionsystem.fashion_system.entity.PromotionTierId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionTier.
 */
public interface PromotionTierRepository extends BaseRepository<PromotionTier, PromotionTierId> {
    List<PromotionTier> findAllByPromotionId(UUID promotionId);

    List<PromotionTier> findAllByPromotionIdIn(java.util.Collection<UUID> promotionIds);

    boolean existsByPromotionId(UUID promotionId);

    boolean existsByPromotionIdAndTierId(UUID promotionId, UUID tierId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PromotionTier t where t.promotionId = :promotionId")
    void deleteAllByPromotionId(@Param("promotionId") UUID promotionId);
}
