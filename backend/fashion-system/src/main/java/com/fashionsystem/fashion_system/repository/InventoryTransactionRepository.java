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
    default Page<InventoryTransaction> search(UUID storeId, UUID variantId, String transactionType,
            String referenceType, UUID referenceId, Pageable pageable) {
        return searchFiltered(storeId != null, storeId, variantId != null, variantId,
                transactionType, referenceType, referenceId != null, referenceId, pageable);
    }

    @Query("""
            select t from InventoryTransaction t
            where (:filterStore = false or t.storeId = :storeId)
              and (:filterVariant = false or t.productVariantId = :variantId)
              and (:transactionType = '' or upper(t.transactionType) = :transactionType)
              and (:referenceType = '' or upper(coalesce(t.referenceType, '')) = :referenceType)
              and (:filterReference = false or t.referenceId = :referenceId)
            """)
    Page<InventoryTransaction> searchFiltered(
            @Param("filterStore") boolean filterStore, @Param("storeId") UUID storeId,
            @Param("filterVariant") boolean filterVariant, @Param("variantId") UUID variantId,
            @Param("transactionType") String transactionType,
            @Param("referenceType") String referenceType,
            @Param("filterReference") boolean filterReference,
            @Param("referenceId") UUID referenceId, Pageable pageable);
}
