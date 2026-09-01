package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PaymentTransaction;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PaymentTransaction.
 */
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransaction, UUID> {
    @Query("""
            select t from PaymentTransaction t
            where (:paymentId is null or t.paymentId = :paymentId)
              and (:type = '' or upper(t.transactionType) = :type)
              and (:status = '' or upper(t.status) = :status)
            """)
    Page<PaymentTransaction> search(@Param("paymentId") UUID paymentId,
                                    @Param("type") String type,
                                    @Param("status") String status,
                                    Pageable pageable);
}
