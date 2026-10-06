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
                (store_id, product_variant_id, available_quantity, online_quantity, reserved_quantity, damaged_quantity, version, updated_at)
            values (:storeId, :variantId, 0, 0, 0, 0, 0, current_timestamp)
            on conflict (store_id, product_variant_id) do nothing
            """, nativeQuery = true)
    int initializeNative(@Param("storeId") UUID storeId, @Param("variantId") UUID variantId);

    default int initialize(UUID storeId, UUID variantId) {
        int inserted = initializeNative(storeId, variantId);
        if (inserted > 0) {
            InventoryBalance row = findForUpdate(storeId, variantId).orElseThrow();
            var mapper = new tools.jackson.databind.ObjectMapper();
            com.fashionsystem.fashion_system.audit.BulkAuditRecorder.record(
                    "inventory_balances", mapper.valueToTree(new InventoryBalanceId(storeId, variantId)).toString(),
                    com.fashionsystem.fashion_system.audit.AuditOperation.INSERT, null, mapper.valueToTree(row));
        }
        return inserted;
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from InventoryBalance b
            where b.storeId = :storeId and b.productVariantId = :variantId
            """)
    Optional<InventoryBalance> findForUpdate(
            @Param("storeId") UUID storeId, @Param("variantId") UUID variantId);

    default Page<InventoryBalance> search(UUID storeId, UUID variantId, Integer lowStockThreshold,
            Pageable pageable) {
        return searchFiltered(storeId != null, storeId, variantId != null, variantId,
                lowStockThreshold != null, lowStockThreshold, pageable);
    }

    @Query("""
            select b from InventoryBalance b
            where (:filterStore = false or b.storeId = :storeId)
              and (:filterVariant = false or b.productVariantId = :variantId)
              and (:filterThreshold = false or b.availableQuantity <= :lowStockThreshold)
            """)
    Page<InventoryBalance> searchFiltered(
            @Param("filterStore") boolean filterStore, @Param("storeId") UUID storeId,
            @Param("filterVariant") boolean filterVariant, @Param("variantId") UUID variantId,
            @Param("filterThreshold") boolean filterThreshold,
            @Param("lowStockThreshold") Integer lowStockThreshold, Pageable pageable);

    default Page<InventoryBalance> searchWarehouse(UUID storeId, UUID variantId, Integer threshold,
            String keyword, UUID categoryId, String stock, Pageable pageable) {
        return searchWarehouseFiltered(storeId, variantId != null, variantId, threshold != null, threshold,
                keyword, categoryId != null, categoryId, stock, pageable);
    }

    @Query("""
            select b from InventoryBalance b
            join ProductVariant v on v.id = b.productVariantId
            join Product p on p.id = v.productId
            where b.storeId = :storeId
            and (:filterVariant = false or b.productVariantId = :variantId)
            and (:filterThreshold = false or b.availableQuantity + b.onlineQuantity <= :threshold)
            and (:filterCategory = false or p.categoryId = :categoryId)
            and (:keyword = '' or lower(p.name) like lower(concat('%',:keyword,'%'))
                or lower(v.sku) like lower(concat('%',:keyword,'%'))
                or lower(coalesce(v.color,'')) like lower(concat('%',:keyword,'%'))
                or lower(coalesce(v.size,'')) like lower(concat('%',:keyword,'%')))
            and (:stock = 'ALL'
                or (:stock = 'LOW' and b.availableQuantity + b.onlineQuantity between 1 and coalesce(:threshold,5))
                or (:stock = 'OUT' and b.availableQuantity + b.onlineQuantity = 0)
                or (:stock = 'IN' and b.availableQuantity + b.onlineQuantity > 0))
            """)
    Page<InventoryBalance> searchWarehouseFiltered(@Param("storeId") UUID storeId,
        @Param("filterVariant") boolean filterVariant, @Param("variantId") UUID variantId,
        @Param("filterThreshold") boolean filterThreshold, @Param("threshold") Integer threshold,
        @Param("keyword") String keyword, @Param("filterCategory") boolean filterCategory,
        @Param("categoryId") UUID categoryId,
        @Param("stock") String stock,Pageable pageable);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.StoreInventoryStatisticsDto(
                :storeId,
                max(s.name),
                count(b),
                coalesce(sum(b.availableQuantity + b.onlineQuantity), 0L),
                coalesce(sum(b.reservedQuantity), 0L),
                coalesce(sum(b.damagedQuantity), 0L),
                coalesce(sum(case when (b.availableQuantity + b.onlineQuantity) > 0 and (b.availableQuantity + b.onlineQuantity) <= :threshold then 1L else 0L end), 0L),
                coalesce(sum(case when (b.availableQuantity + b.onlineQuantity) = 0 then 1L else 0L end), 0L),
                coalesce(sum(b.availableQuantity),0L),
                coalesce(sum(b.onlineQuantity),0L))
            from InventoryBalance b
            join Store s on s.id = b.storeId
            where b.storeId = :storeId
            """)
    StoreInventoryStatisticsDto summarize(
            @Param("storeId") UUID storeId, @Param("threshold") int threshold);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.StoreInventoryStatisticsDto(
                b.storeId,
                max(s.name),
                count(b),
                coalesce(sum(b.availableQuantity + b.onlineQuantity), 0L),
                coalesce(sum(b.reservedQuantity), 0L),
                coalesce(sum(b.damagedQuantity), 0L),
                coalesce(sum(case when (b.availableQuantity + b.onlineQuantity) > 0 and (b.availableQuantity + b.onlineQuantity) <= :threshold then 1L else 0L end), 0L),
                coalesce(sum(case when (b.availableQuantity + b.onlineQuantity) = 0 then 1L else 0L end), 0L),
                coalesce(sum(b.availableQuantity),0L),
                coalesce(sum(b.onlineQuantity),0L))
            from InventoryBalance b
            join Store s on s.id = b.storeId
            group by b.storeId
            order by b.storeId
            """)
    List<StoreInventoryStatisticsDto> summarizeByStore(@Param("threshold") int threshold);
}
