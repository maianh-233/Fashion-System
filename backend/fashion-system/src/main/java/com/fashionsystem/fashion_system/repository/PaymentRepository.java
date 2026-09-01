package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Payment;
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
 * Cung cấp các thao tác CRUD cơ bản cho Payment.
 */
public interface PaymentRepository extends BaseRepository<Payment, UUID> {
    boolean existsByPaymentCode(String paymentCode);
    boolean existsByPaymentCodeAndIdNot(String paymentCode, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select p from Payment p
            where (:orderId is null or p.orderId = :orderId)
              and (:method = '' or upper(p.method) = :method)
              and (:status = '' or upper(p.status) = :status)
              and (:keyword = ''
                or lower(coalesce(p.paymentCode, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.transactionCode, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<Payment> search(@Param("orderId") UUID orderId, @Param("method") String method,
                         @Param("status") String status, @Param("keyword") String keyword,
                         Pageable pageable);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.orderId = :orderId and p.status = 'SUCCESS'")
    BigDecimal sumSuccessfulAmount(@Param("orderId") UUID orderId);
}
