package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.StockReservation;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho StockReservation.
 */
public interface StockReservationRepository extends BaseRepository<StockReservation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockReservation r where r.id = :id")
    Optional<StockReservation> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByOrderIdAndStoreIdAndProductVariantIdAndStatus(
            UUID orderId, UUID storeId, UUID productVariantId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockReservation> findByOrderIdAndStoreIdAndProductVariantIdAndStatus(
            UUID orderId, UUID storeId, UUID productVariantId, String status);

    @Query("""
            select r from StockReservation r
            where (:orderId is null or r.orderId = :orderId)
              and (:storeId is null or r.storeId = :storeId)
              and (:variantId is null or r.productVariantId = :variantId)
              and (:status = '' or upper(coalesce(r.status, '')) = :status)
            """)
    Page<StockReservation> search(
            @Param("orderId") UUID orderId, @Param("storeId") UUID storeId,
            @Param("variantId") UUID variantId, @Param("status") String status,
            Pageable pageable);
}
