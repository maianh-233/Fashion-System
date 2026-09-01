package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ProductAttribute;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho ProductAttribute.
 */
public interface ProductAttributeRepository extends BaseRepository<ProductAttribute, UUID> {
    Optional<ProductAttribute> findByIdAndProductId(UUID id, UUID productId);

    @Query("""
            select a from ProductAttribute a
            where a.productId = :productId
              and (:keyword = ''
                or lower(a.attributeName) like lower(concat('%', :keyword, '%'))
                or lower(a.attributeValue) like lower(concat('%', :keyword, '%')))
            """)
    Page<ProductAttribute> searchByProduct(
            @Param("productId") UUID productId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
