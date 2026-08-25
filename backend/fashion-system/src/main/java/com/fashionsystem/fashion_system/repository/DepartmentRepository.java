package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Department;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends BaseRepository<Department, UUID> {
    Optional<Department> findByCode(String code);
}
