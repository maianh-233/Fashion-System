package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.AttendanceAuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, UUID> {
}
