package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Collection.
 */
public interface CollectionRepository extends BaseRepository<Collection, UUID> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("""
            select c from Collection c
            where (:keyword = ''
                or lower(c.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.code, '')) like lower(concat('%', :keyword, '%')))
              and (:brandId is null or c.brandId = :brandId)
              and (:season = '' or upper(c.season) = :season)
              and (:year is null or c.year = :year)
              and (:status = '' or upper(c.status) = :status)
            """)
    Page<Collection> search(
            @Param("keyword") String keyword,
            @Param("brandId") UUID brandId,
            @Param("season") String season,
            @Param("year") Integer year,
            @Param("status") String status,
            Pageable pageable);
}
