package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Product;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Product.
 */
public interface ProductRepository extends BaseRepository<Product, UUID> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Query("""
            select p from Product p
            where (:keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.slug, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.material, '')) like lower(concat('%', :keyword, '%')))
              and (:brandId is null or p.brandId = :brandId)
              and (:collectionId is null or p.collectionId = :collectionId)
              and (:categoryId is null or p.categoryId = :categoryId)
              and (:gender = '' or upper(coalesce(p.gender, '')) = :gender)
              and (:status = '' or upper(coalesce(p.status, '')) = :status)
            """)
    Page<Product> search(
            @Param("keyword") String keyword,
            @Param("brandId") UUID brandId,
            @Param("collectionId") UUID collectionId,
            @Param("categoryId") UUID categoryId,
            @Param("gender") String gender,
            @Param("status") String status,
            Pageable pageable);
}
