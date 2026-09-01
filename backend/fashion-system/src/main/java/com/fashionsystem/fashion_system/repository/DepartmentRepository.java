package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Department;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends BaseRepository<Department, UUID> {
    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("""
            select d from Department d
            where (:keyword = ''
                or lower(d.name) like lower(concat('%', :keyword, '%'))
                or lower(d.code) like lower(concat('%', :keyword, '%')))
              and (:active is null or d.active = :active)
            """)
    Page<Department> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable);
}
