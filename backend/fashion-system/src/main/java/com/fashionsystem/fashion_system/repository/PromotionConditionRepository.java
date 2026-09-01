package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionCondition;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionCondition.
 */
public interface PromotionConditionRepository extends BaseRepository<PromotionCondition, UUID> {
    Optional<PromotionCondition> findByIdAndPromotionId(UUID id, UUID promotionId);
    Page<PromotionCondition> findAllByPromotionId(UUID promotionId, Pageable pageable);
}
