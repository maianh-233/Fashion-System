package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionProduct;
import com.fashionsystem.fashion_system.entity.PromotionProductId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionProduct.
 */
public interface PromotionProductRepository extends BaseRepository<PromotionProduct, PromotionProductId> {
    List<PromotionProduct> findAllByPromotionId(UUID promotionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PromotionProduct p where p.promotionId = :promotionId")
    void deleteAllByPromotionId(@Param("promotionId") UUID promotionId);
}
