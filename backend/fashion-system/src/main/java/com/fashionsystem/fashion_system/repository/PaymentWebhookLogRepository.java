package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.PaymentWebhookLog;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho PaymentWebhookLog.
 */
public interface PaymentWebhookLogRepository extends BaseRepository<PaymentWebhookLog, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PaymentWebhookLog w where w.id = :id")
    Optional<PaymentWebhookLog> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select w from PaymentWebhookLog w
            where (:provider = '' or upper(w.provider) = :provider)
              and (:eventType = '' or upper(coalesce(w.eventType, '')) = :eventType)
              and (:processed is null or w.processed = :processed)
            """)
    Page<PaymentWebhookLog> search(@Param("provider") String provider,
                                   @Param("eventType") String eventType,
                                   @Param("processed") Boolean processed,
                                   Pageable pageable);
}
