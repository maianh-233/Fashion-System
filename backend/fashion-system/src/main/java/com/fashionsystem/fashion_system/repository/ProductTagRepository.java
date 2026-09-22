package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ProductTag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho ProductTag.
 */
public interface ProductTagRepository extends BaseRepository<ProductTag, UUID> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    @Query("""
            select t from ProductTag t
            where (:keyword = '' or lower(t.name) like lower(concat('%', :keyword, '%')))
              and (:active is null or t.active = :active)
            """)
    Page<ProductTag> search(@Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
