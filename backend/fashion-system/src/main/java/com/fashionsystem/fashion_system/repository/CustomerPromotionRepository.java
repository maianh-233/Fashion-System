package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerPromotion;
import jakarta.persistence.LockModeType;
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

    /** Voucher đổi điểm khách đang sở hữu và chưa dùng. */
    List<CustomerPromotion> findAllByCustomerIdAndUsedOrderIdIsNull(UUID customerId);

    /** Khóa một voucher đổi điểm chưa dùng (cũ nhất trước) để đánh dấu đã dùng khi tạo đơn. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from CustomerPromotion c
            where c.customerId = :customerId and c.promotionId = :promotionId and c.usedOrderId is null
            order by c.redeemedAt asc
            """)
    List<CustomerPromotion> findUnusedForUpdate(@Param("customerId") UUID customerId,
                                                @Param("promotionId") UUID promotionId);
}
