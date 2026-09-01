package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.InventoryTransaction;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho InventoryTransaction.
 */
public interface InventoryTransactionRepository extends BaseRepository<InventoryTransaction, UUID> {
    @Query("""
            select t from InventoryTransaction t
            where (:storeId is null or t.storeId = :storeId)
              and (:variantId is null or t.productVariantId = :variantId)
              and (:transactionType = '' or upper(t.transactionType) = :transactionType)
              and (:referenceType = '' or upper(coalesce(t.referenceType, '')) = :referenceType)
              and (:referenceId is null or t.referenceId = :referenceId)
            """)
    Page<InventoryTransaction> search(
            @Param("storeId") UUID storeId, @Param("variantId") UUID variantId,
            @Param("transactionType") String transactionType,
            @Param("referenceType") String referenceType,
            @Param("referenceId") UUID referenceId, Pageable pageable);
}
