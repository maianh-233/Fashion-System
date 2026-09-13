package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Position;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PositionRepository extends BaseRepository<Position, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    boolean existsByDepartmentId(UUID departmentId);
    List<Position> findAllByDepartmentIdOrderByNameAsc(UUID departmentId);

    @Query("""
            select p from Position p
            where (:keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.code) like lower(concat('%', :keyword, '%')))
              and (:departmentId is null or p.departmentId = :departmentId)
              and (:active is null or p.active = :active)
            """)
    Page<Position> search(
            @Param("keyword") String keyword,
            @Param("departmentId") UUID departmentId,
            @Param("active") Boolean active,
            Pageable pageable);
}
