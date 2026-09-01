package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.InventoryBalance;
import com.fashionsystem.fashion_system.entity.InventoryBalanceId;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho InventoryBalance.
 */
public interface InventoryBalanceRepository extends BaseRepository<InventoryBalance, InventoryBalanceId> {
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into inventory_balances
                (store_id, product_variant_id, available_quantity, reserved_quantity, damaged_quantity, updated_at)
            values (:storeId, :variantId, 0, 0, 0, current_timestamp)
            on conflict (store_id, product_variant_id) do nothing
            """, nativeQuery = true)
    int initialize(@Param("storeId") UUID storeId, @Param("variantId") UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from InventoryBalance b
            where b.storeId = :storeId and b.productVariantId = :variantId
            """)
    Optional<InventoryBalance> findForUpdate(
            @Param("storeId") UUID storeId, @Param("variantId") UUID variantId);

    @Query("""
            select b from InventoryBalance b
            where (:storeId is null or b.storeId = :storeId)
              and (:variantId is null or b.productVariantId = :variantId)
              and (:lowStockThreshold is null or b.availableQuantity <= :lowStockThreshold)
            """)
    Page<InventoryBalance> search(
            @Param("storeId") UUID storeId, @Param("variantId") UUID variantId,
            @Param("lowStockThreshold") Integer lowStockThreshold, Pageable pageable);
}
