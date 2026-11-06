package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AttendanceRecord;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    Optional<AttendanceRecord> findByUserIdAndAttendanceDate(UUID userId, LocalDate attendanceDate);
}
