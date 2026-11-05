package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Shift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findByStoreIdAndActive(UUID storeId, Boolean active);

    Optional<Shift> findByStoreIdAndCode(UUID storeId, String code);

    @Query("""
            select s from Shift s
            where s.storeId = :storeId
              and (s.active = true or :includeInactive = true)
            order by s.code
            """)
    List<Shift> findByStore(@Param("storeId") UUID storeId, @Param("includeInactive") Boolean includeInactive);
}
