package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Product;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Product.
 */
public interface ProductRepository extends BaseRepository<Product, UUID> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    long countByBrandId(UUID brandId);
    long countByCategoryId(UUID categoryId);
    long countByCollectionId(UUID collectionId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.brandId = :id")
    java.util.List<Product> findRowsToArchiveByBrandId(@Param("id") UUID id);

    default int archiveByBrandId(UUID id) {
        var rows = findRowsToArchiveByBrandId(id);
        var now = java.time.LocalDateTime.now();
        rows.forEach(row -> { row.setStatus("ARCHIVE"); row.setUpdatedAt(now); });
        saveAll(rows);
        return rows.size();
    }

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.categoryId = :id")
    java.util.List<Product> findRowsToArchiveByCategoryId(@Param("id") UUID id);

    default int archiveByCategoryId(UUID id) {
        var rows = findRowsToArchiveByCategoryId(id);
        var now = java.time.LocalDateTime.now();
        rows.forEach(row -> { row.setStatus("ARCHIVE"); row.setUpdatedAt(now); });
        saveAll(rows);
        return rows.size();
    }

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.collectionId = :id")
    java.util.List<Product> findRowsToArchiveByCollectionId(@Param("id") UUID id);

    default int archiveByCollectionId(UUID id) {
        var rows = findRowsToArchiveByCollectionId(id);
        var now = java.time.LocalDateTime.now();
        rows.forEach(row -> { row.setStatus("ARCHIVE"); row.setUpdatedAt(now); });
        saveAll(rows);
        return rows.size();
    }

    @Query("""
            select p from Product p
            where (:keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.slug, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.code, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.material, '')) like lower(concat('%', :keyword, '%')))
              and (:brandId is null or p.brandId = :brandId)
              and (:collectionId is null or p.collectionId = :collectionId)
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:tagId is null or exists (select m from ProductTagMapping m where m.productId = p.id and m.tagId = :tagId))
              and (:gender = '' or upper(coalesce(p.gender, '')) = :gender)
              and (:status = '' or upper(coalesce(p.status, '')) = :status)
            """)
    Page<Product> search(
            @Param("keyword") String keyword,
            @Param("brandId") UUID brandId,
            @Param("collectionId") UUID collectionId,
            @Param("categoryId") UUID categoryId,
            @Param("tagId") UUID tagId,
            @Param("gender") String gender,
            @Param("status") String status,
            Pageable pageable);
}
