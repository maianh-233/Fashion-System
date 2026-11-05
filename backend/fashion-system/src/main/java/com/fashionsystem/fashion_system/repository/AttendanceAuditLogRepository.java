package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AttendanceAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, UUID> {
    @Query("""
            select aal from AttendanceAuditLog aal
            where aal.attendanceRecordId = :attendanceRecordId
            order by aal.createdAt desc
            """)
    List<AttendanceAuditLog> findByAttendanceRecordIdOrderByCreatedAtDesc(@Param("attendanceRecordId") UUID attendanceRecordId);

    @Query("""
            select aal from AttendanceAuditLog aal
            where aal.attendanceRecordId in :recordIds
            order by aal.createdAt desc
            """)
    List<AttendanceAuditLog> findByAttendanceRecordIdIn(@Param("recordIds") List<UUID> recordIds);
}
