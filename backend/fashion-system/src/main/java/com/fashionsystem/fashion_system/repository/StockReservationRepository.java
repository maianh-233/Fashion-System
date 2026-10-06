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

    default Page<StockReservation> search(UUID orderId, UUID storeId, UUID variantId,
            String status, Pageable pageable) {
        return searchFiltered(orderId != null, orderId, storeId != null, storeId,
                variantId != null, variantId, status, pageable);
    }

    @Query("""
            select r from StockReservation r
            where (:filterOrder = false or r.orderId = :orderId)
              and (:filterStore = false or r.storeId = :storeId)
              and (:filterVariant = false or r.productVariantId = :variantId)
              and (:status = '' or upper(coalesce(r.status, '')) = :status)
            """)
    Page<StockReservation> searchFiltered(
            @Param("filterOrder") boolean filterOrder, @Param("orderId") UUID orderId,
            @Param("filterStore") boolean filterStore, @Param("storeId") UUID storeId,
            @Param("filterVariant") boolean filterVariant, @Param("variantId") UUID variantId,
            @Param("status") String status,
            Pageable pageable);
}
