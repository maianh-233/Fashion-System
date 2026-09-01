package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Refund;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Refund.
 */
public interface RefundRepository extends BaseRepository<Refund, UUID> {
    boolean existsByRefundCode(String refundCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :id")
    Optional<Refund> findByIdForUpdate(@Param("id") UUID id);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.paymentId = :paymentId and r.status = 'COMPLETED'")
    BigDecimal sumCompletedAmount(@Param("paymentId") UUID paymentId);

    @Query("select coalesce(sum(r.amount), 0) from Refund r where r.paymentId = :paymentId and r.status in ('PENDING', 'COMPLETED')")
    BigDecimal sumCommittedAmount(@Param("paymentId") UUID paymentId);

    @Query("""
            select r from Refund r
            where (:paymentId is null or r.paymentId = :paymentId)
              and (:status = '' or upper(r.status) = :status)
              and (:keyword = '' or lower(coalesce(r.refundCode, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<Refund> search(@Param("paymentId") UUID paymentId, @Param("status") String status,
                        @Param("keyword") String keyword, Pageable pageable);
}
