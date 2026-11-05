package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    Optional<AttendanceRecord> findByUserIdAndAttendanceDate(UUID userId, LocalDate attendanceDate);

    @Query("""
            select ar from AttendanceRecord ar
            where ar.userId = :userId
              and ar.attendanceDate between :fromDate and :toDate
            order by ar.attendanceDate desc
            """)
    List<AttendanceRecord> findByUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            select ar from AttendanceRecord ar
            where ar.storeId = :storeId
              and ar.attendanceDate = :date
            order by ar.userId
            """)
    List<AttendanceRecord> findByStoreAndDate(@Param("storeId") UUID storeId, @Param("date") LocalDate date);

    @Query("""
            select ar from AttendanceRecord ar
            where (ar.storeId = :storeId or (:storeId is null))
              and ar.attendanceDate between :fromDate and :toDate
              and (:status is null or ar.status = :status)
            """)
    Page<AttendanceRecord> searchRecords(
            @Param("storeId") UUID storeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
            select ar from AttendanceRecord ar
            where ar.userId = :userId
              and ar.attendanceDate = :date
            """)
    Optional<AttendanceRecord> findTodayRecord(@Param("userId") UUID userId, @Param("date") LocalDate date);
}
