package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho CustomerPromotion.
 */
public interface CustomerPromotionRepository extends BaseRepository<CustomerPromotion, UUID> {
    boolean existsByPromotionId(UUID promotionId);

    long countByPromotionId(UUID promotionId);

    Page<CustomerPromotion> findAllByCustomerId(UUID customerId, Pageable pageable);
}
