package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ExcuseRequest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcuseRequestRepository extends JpaRepository<ExcuseRequest, UUID> {
}
