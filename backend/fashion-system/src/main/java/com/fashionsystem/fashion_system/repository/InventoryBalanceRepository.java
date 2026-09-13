package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.InventoryBalance;
import com.fashionsystem.fashion_system.entity.InventoryBalanceId;
import com.fashionsystem.fashion_system.dto.StoreInventoryStatisticsDto;
import java.util.List;
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

    @Query("""
            select new com.fashionsystem.fashion_system.dto.StoreInventoryStatisticsDto(
                :storeId,
                max(case when :storeId is not null then s.name else null end),
                count(b),
                coalesce(sum(b.availableQuantity), 0L),
                coalesce(sum(b.reservedQuantity), 0L),
                coalesce(sum(b.damagedQuantity), 0L),
                coalesce(sum(case when b.availableQuantity > 0 and b.availableQuantity <= :threshold then 1L else 0L end), 0L),
                coalesce(sum(case when b.availableQuantity = 0 then 1L else 0L end), 0L))
            from InventoryBalance b
            join Store s on s.id = b.storeId
            where (:storeId is null or b.storeId = :storeId)
            """)
    StoreInventoryStatisticsDto summarize(
            @Param("storeId") UUID storeId, @Param("threshold") int threshold);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.StoreInventoryStatisticsDto(
                b.storeId,
                max(s.name),
                count(b),
                coalesce(sum(b.availableQuantity), 0L),
                coalesce(sum(b.reservedQuantity), 0L),
                coalesce(sum(b.damagedQuantity), 0L),
                coalesce(sum(case when b.availableQuantity > 0 and b.availableQuantity <= :threshold then 1L else 0L end), 0L),
                coalesce(sum(case when b.availableQuantity = 0 then 1L else 0L end), 0L))
            from InventoryBalance b
            join Store s on s.id = b.storeId
            group by b.storeId
            order by b.storeId
            """)
    List<StoreInventoryStatisticsDto> summarizeByStore(@Param("threshold") int threshold);
}
