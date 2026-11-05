package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.LeaveType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    Optional<LeaveType> findByCode(String code);

    List<LeaveType> findByActiveTrue();

    List<LeaveType> findByActiveTrueOrderByCode();
}
