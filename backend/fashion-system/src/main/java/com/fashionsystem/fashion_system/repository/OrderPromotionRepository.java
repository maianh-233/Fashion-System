package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderPromotion;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho OrderPromotion.
 */
public interface OrderPromotionRepository extends BaseRepository<OrderPromotion, UUID> {
    boolean existsByOrderIdAndPromotionId(UUID orderId, UUID promotionId);
    Optional<OrderPromotion> findByIdAndOrderId(UUID id, UUID orderId);
    List<OrderPromotion> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

    @Query("select coalesce(sum(p.discountAmount), 0) from OrderPromotion p where p.orderId = :orderId")
    BigDecimal sumDiscount(@Param("orderId") UUID orderId);
}
