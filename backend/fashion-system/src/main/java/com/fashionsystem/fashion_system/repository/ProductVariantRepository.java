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

    @Modifying
    @Query("update ProductVariant v set v.active = false, v.updatedAt = CURRENT_TIMESTAMP where v.productId = :id")
    int deactivateByProductId(@Param("id") UUID id);

    @Modifying
    @Query("update ProductVariant v set v.active = false, v.updatedAt = CURRENT_TIMESTAMP where v.productId in (select p.id from Product p where p.brandId = :id)")
    int deactivateByBrandId(@Param("id") UUID id);

    @Modifying
    @Query("update ProductVariant v set v.active = false, v.updatedAt = CURRENT_TIMESTAMP where v.productId in (select p.id from Product p where p.categoryId = :id)")
    int deactivateByCategoryId(@Param("id") UUID id);

    @Modifying
    @Query("update ProductVariant v set v.active = false, v.updatedAt = CURRENT_TIMESTAMP where v.productId in (select p.id from Product p where p.collectionId = :id)")
    int deactivateByCollectionId(@Param("id") UUID id);

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
              and (:size = '' or lower(coalesce(v.size, '')) = lower(:size))
              and (:active is null or v.active = :active)
              and (:minPrice is null or v.price >= :minPrice)
              and (:maxPrice is null or v.price <= :maxPrice)
            """)
    Page<ProductVariant> searchByProduct(
            @Param("productId") UUID productId,
            @Param("keyword") String keyword,
            @Param("color") String color,
            @Param("size") String size,
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
              and (:size = '' or lower(coalesce(v.size, '')) = lower(:size))
              and (:active is null or v.active = :active)
            """)
    Page<ProductVariant> searchAll(@Param("productId") UUID productId,
            @Param("keyword") String keyword, @Param("color") String color,
            @Param("size") String size, @Param("active") Boolean active, Pageable pageable);
}
