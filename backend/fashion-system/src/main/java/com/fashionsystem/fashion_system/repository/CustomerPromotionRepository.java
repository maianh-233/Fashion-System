package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho CustomerPromotion.
 */
public interface CustomerPromotionRepository extends BaseRepository<CustomerPromotion, UUID> {
    boolean existsByPromotionId(UUID promotionId);

    long countByPromotionId(UUID promotionId);

    Page<CustomerPromotion> findAllByCustomerId(UUID customerId, Pageable pageable);

    /** Voucher riêng khách đang sở hữu, chưa dùng và đang trong hạn riêng (nếu có) tại {@code at}. */
    @Query("""
            select c from CustomerPromotion c
            where c.customerId = :customerId and c.usedOrderId is null
              and (c.validFrom is null or c.validFrom <= :at) and (c.expiresAt is null or c.expiresAt > :at)
            """)
    List<CustomerPromotion> findUsable(@Param("customerId") UUID customerId, @Param("at") LocalDateTime at);

    /** Khóa một voucher riêng còn dùng được (sắp hết hạn / cũ nhất trước) để đánh dấu đã dùng khi tạo đơn. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from CustomerPromotion c
            where c.customerId = :customerId and c.promotionId = :promotionId and c.usedOrderId is null
              and (c.validFrom is null or c.validFrom <= :at) and (c.expiresAt is null or c.expiresAt > :at)
            order by c.expiresAt asc nulls last, c.redeemedAt asc
            """)
    List<CustomerPromotion> findUnusedForUpdate(@Param("customerId") UUID customerId,
                                                @Param("promotionId") UUID promotionId,
                                                @Param("at") LocalDateTime at);
}
