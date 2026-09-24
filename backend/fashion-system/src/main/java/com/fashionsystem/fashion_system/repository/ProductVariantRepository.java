package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho ProductVariant.
 */
public interface ProductVariantRepository extends BaseRepository<ProductVariant, UUID> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

    long countByProductId(UUID productId);

    @Query("select count(v) > 0 from ProductVariant v where v.productId = :productId and lower(coalesce(trim(v.color), '')) = :color and lower(coalesce(trim(v.size), '')) = :size and (:excludedId is null or v.id <> :excludedId)")
    boolean existsCombination(@Param("productId") UUID productId, @Param("color") String color,
            @Param("size") String size, @Param("excludedId") UUID excludedId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.productId = :id")
    java.util.List<ProductVariant> findRowsToDeactivateByProductId(@Param("id") UUID id);

    default int deactivateByProductId(UUID id) {
        var rows = findRowsToDeactivateByProductId(id);
        var now = java.time.LocalDateTime.now();
        rows.forEach(row -> { row.setActive(false); row.setUpdatedAt(now); });
        saveAll(rows);
        return rows.size();
    }

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.productId in (select p.id from Product p where p.brandId = :id)")
    java.util.List<ProductVariant> findRowsToDeactivateByBrandId(@Param("id") UUID id);

    default int deactivateByBrandId(UUID id) {
        var rows = findRowsToDeactivateByBrandId(id);
        var now = java.time.LocalDateTime.now();
        rows.forEach(row -> { row.setActive(false); row.setUpdatedAt(now); });
        saveAll(rows);
        return rows.size();
    }

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.productId in (select p.id from Product p where p.categoryId = :id)")
    java.util.List<ProductVariant> findRowsToDeactivateByCategoryId(@Param("id") UUID id);

    default int deactivateByCategoryId(UUID id) {
        var rows = findRowsToDeactivateByCategoryId(id);
        var now = java.time.LocalDateTime.now();
        rows.forEach(row -> { row.setActive(false); row.setUpdatedAt(now); });
        saveAll(rows);
        return rows.size();
    }

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.productId in (select p.id from Product p where p.collectionId = :id)")
    java.util.List<ProductVariant> findRowsToDeactivateByCollectionId(@Param("id") UUID id);

    default int deactivateByCollectionId(UUID id) {
        var rows = findRowsToDeactivateByCollectionId(id);
        var now = java.time.LocalDateTime.now();
        rows.forEach(row -> { row.setActive(false); row.setUpdatedAt(now); });
        saveAll(rows);
        return rows.size();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select v from ProductVariant v
            where v.productId = :productId
              and (:keyword = ''
                or lower(v.sku) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(v.barcode, '')) like lower(concat('%', :keyword, '%')))
              and (:color = '' or lower(coalesce(v.color, '')) = lower(:color))
              and (:variantSize = '' or lower(coalesce(v.size, '')) = lower(:variantSize))
              and (:active is null or v.active = :active)
              and (:minPrice is null or v.price >= :minPrice)
              and (:maxPrice is null or v.price <= :maxPrice)
            """)
    Page<ProductVariant> searchByProduct(
            @Param("productId") UUID productId,
            @Param("keyword") String keyword,
            @Param("color") String color,
            @Param("variantSize") String variantSize,
            @Param("active") Boolean active,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query("""
            select v from ProductVariant v join Product p on p.id = v.productId
            where (:productId is null or v.productId = :productId)
              and (:keyword = '' or lower(v.sku) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(v.barcode, '')) like lower(concat('%', :keyword, '%'))
                   or lower(p.name) like lower(concat('%', :keyword, '%')))
              and (:color = '' or lower(coalesce(v.color, '')) = lower(:color))
              and (:variantSize = '' or lower(coalesce(v.size, '')) = lower(:variantSize))
              and (:active is null or v.active = :active)
            """)
    Page<ProductVariant> searchAll(@Param("productId") UUID productId,
            @Param("keyword") String keyword, @Param("color") String color,
            @Param("variantSize") String variantSize, @Param("active") Boolean active, Pageable pageable);
}
