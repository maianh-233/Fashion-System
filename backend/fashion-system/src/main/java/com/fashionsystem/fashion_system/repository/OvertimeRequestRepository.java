package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OvertimeRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest, UUID> {
}
