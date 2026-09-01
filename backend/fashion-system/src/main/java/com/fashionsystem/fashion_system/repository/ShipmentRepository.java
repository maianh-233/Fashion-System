package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Shipment;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Shipment.
 */
public interface ShipmentRepository extends BaseRepository<Shipment, UUID> {
    boolean existsByOrderId(UUID orderId);
    Optional<Shipment> findByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :id")
    Optional<Shipment> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select s from Shipment s
            where (:orderId is null or s.orderId = :orderId)
              and (:status = '' or upper(coalesce(s.shippingStatus, '')) = :status)
              and (:provider = '' or lower(coalesce(s.shippingProvider, '')) = lower(:provider))
            """)
    Page<Shipment> search(@Param("orderId") UUID orderId, @Param("status") String status,
                          @Param("provider") String provider, Pageable pageable);
}
