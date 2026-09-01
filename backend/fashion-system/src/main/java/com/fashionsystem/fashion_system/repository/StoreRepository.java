package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Store;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Store.
 */
public interface StoreRepository extends BaseRepository<Store, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("""
            select s from Store s
            where (:keyword = ''
                or lower(s.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.code, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.phone, '')) like lower(concat('%', :keyword, '%')))
              and (:active is null or s.active = :active)
            """)
    Page<Store> search(@Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
