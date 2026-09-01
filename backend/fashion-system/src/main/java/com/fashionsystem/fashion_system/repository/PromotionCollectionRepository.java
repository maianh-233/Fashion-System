package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionCollection;
import com.fashionsystem.fashion_system.entity.PromotionCollectionId;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionCollection.
 */
public interface PromotionCollectionRepository extends BaseRepository<PromotionCollection, PromotionCollectionId> {
    boolean existsByPromotionIdAndCollectionId(UUID promotionId, UUID collectionId);
    void deleteByPromotionIdAndCollectionId(UUID promotionId, UUID collectionId);
    Page<PromotionCollection> findAllByPromotionId(UUID promotionId, Pageable pageable);
}
