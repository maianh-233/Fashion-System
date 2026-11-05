package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.EmployeeShift;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, UUID> {
    @Query("""
            select es from EmployeeShift es
            where es.userId = :userId
              and es.startDate <= :date
              and (es.endDate is null or es.endDate >= :date)
            order by es.startDate desc
            """)
    Optional<EmployeeShift> findActiveOnDate(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("""
            select es from EmployeeShift es
            where es.userId = :userId
            order by es.startDate desc
            """)
    List<EmployeeShift> findByUserIdOrderByStartDateDesc(@Param("userId") UUID userId);
}
