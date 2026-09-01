package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Brand;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Brand.
 */
public interface BrandRepository extends BaseRepository<Brand, UUID> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("""
            select b from Brand b
            where (:keyword = ''
                or lower(b.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(b.code, '')) like lower(concat('%', :keyword, '%')))
              and (:status = '' or upper(b.status) = :status)
            """)
    Page<Brand> search(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);
}
