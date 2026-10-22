package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Shipment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Các chuyến giao hàng của đơn. */
public interface ShipmentRepository extends BaseRepository<Shipment, UUID> {
    List<Shipment> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);

    long countByOrderId(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.shippingProvider = :provider and s.providerOrderId = :providerOrderId")
    Optional<Shipment> findByProviderOrderForUpdate(@Param("provider") String provider,
                                                    @Param("providerOrderId") String providerOrderId);
}
