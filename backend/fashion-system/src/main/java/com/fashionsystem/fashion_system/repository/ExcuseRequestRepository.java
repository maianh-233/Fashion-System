package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ExcuseRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExcuseRequestRepository extends JpaRepository<ExcuseRequest, UUID> {
    Optional<ExcuseRequest> findByCode(String code);

    @Query("""
            select er from ExcuseRequest er
            where er.userId = :userId
              and er.attendanceDate = :date
              and er.excuseType = :excuseType
            """)
    Optional<ExcuseRequest> findExisting(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date,
            @Param("excuseType") String excuseType);

    @Query("""
            select er from ExcuseRequest er
            where er.userId = :userId
            order by er.createdAt desc
            """)
    List<ExcuseRequest> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("""
            select er from ExcuseRequest er
            where (:status is null or er.status = :status)
              and er.createdAt between :fromDate and :toDate
            order by er.createdAt desc
            """)
    Page<ExcuseRequest> searchRequests(
            @Param("status") String status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
