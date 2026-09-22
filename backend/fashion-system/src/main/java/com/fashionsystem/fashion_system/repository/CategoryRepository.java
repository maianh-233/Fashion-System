package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Category;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Category.
 */
public interface CategoryRepository extends BaseRepository<Category, UUID> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
    boolean existsByParentIdAndActiveTrue(UUID parentId);
    long countByParentIdAndActiveTrue(UUID parentId);

    @Query("""
            select c from Category c
            where (:keyword = ''
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.code, '')) like lower(concat('%', :keyword, '%')))
              and (:parentId is null or c.parentId = :parentId)
              and (:active is null or c.active = :active)
            """)
    Page<Category> search(
            @Param("keyword") String keyword,
            @Param("parentId") UUID parentId,
            @Param("active") Boolean active,
            Pageable pageable);
}
