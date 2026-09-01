package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ProductTagMapping;
import com.fashionsystem.fashion_system.entity.ProductTagMappingId;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.entity.ProductTag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho ProductTagMapping.
 */
public interface ProductTagMappingRepository extends BaseRepository<ProductTagMapping, ProductTagMappingId> {
    boolean existsByProductIdAndTagId(UUID productId, UUID tagId);

    void deleteByProductIdAndTagId(UUID productId, UUID tagId);

    @Query("""
            select t from ProductTag t
            where exists (
                select m from ProductTagMapping m
                where m.productId = :productId and m.tagId = t.id)
            """)
    Page<ProductTag> findTagsByProductId(@Param("productId") UUID productId, Pageable pageable);

    @Query("""
            select p from Product p
            where exists (
                select m from ProductTagMapping m
                where m.tagId = :tagId and m.productId = p.id)
            """)
    Page<Product> findProductsByTagId(@Param("tagId") UUID tagId, Pageable pageable);
}
