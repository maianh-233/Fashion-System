package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.LeaveRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    Optional<LeaveRequest> findByCode(String code);

    @Query("""
            select lr from LeaveRequest lr
            where lr.userId = :userId
            order by lr.startDate desc
            """)
    List<LeaveRequest> findByUserIdOrderByStartDateDesc(@Param("userId") UUID userId);

    @Query("""
            select lr from LeaveRequest lr
            where lr.userId = :userId
              and lr.startDate <= :endDate
              and lr.endDate >= :startDate
              and lr.status != 'REJECTED'
            order by lr.startDate
            """)
    List<LeaveRequest> findOverlappingLeaves(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select lr from LeaveRequest lr
            where (:status is null or lr.status = :status)
              and lr.startDate between :fromDate and :toDate
            order by lr.startDate desc
            """)
    Page<LeaveRequest> searchRequests(
            @Param("status") String status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
