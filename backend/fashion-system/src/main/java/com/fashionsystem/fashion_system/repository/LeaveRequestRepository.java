package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.LeaveRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
}
