package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PromotionUsage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PromotionUsage.
 */
public interface PromotionUsageRepository extends BaseRepository<PromotionUsage, UUID> {
    boolean existsByPromotionId(UUID promotionId);
    boolean existsByPromotionIdAndOrderId(UUID promotionId, UUID orderId);
    long countByPromotionId(UUID promotionId);
    long countByPromotionIdAndCustomerId(UUID promotionId, UUID customerId);

    @Query("""
            select u from PromotionUsage u
            where (:promotionId is null or u.promotionId = :promotionId)
              and (:orderId is null or u.orderId = :orderId)
              and (:customerId is null or u.customerId = :customerId)
              and (:fromDate is null or u.usedAt >= :fromDate)
              and (:toDate is null or u.usedAt <= :toDate)
            """)
    Page<PromotionUsage> search(
            @Param("promotionId") UUID promotionId, @Param("orderId") UUID orderId,
            @Param("customerId") UUID customerId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate, Pageable pageable);
}
