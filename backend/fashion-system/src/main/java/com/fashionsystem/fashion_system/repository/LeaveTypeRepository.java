package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.LeaveType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    boolean existsByCodeIgnoreCase(String code);
}
