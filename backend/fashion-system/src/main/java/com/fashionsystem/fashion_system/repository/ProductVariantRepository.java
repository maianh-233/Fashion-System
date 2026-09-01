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
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho ProductVariant.
 */
public interface ProductVariantRepository extends BaseRepository<ProductVariant, UUID> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

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
}
